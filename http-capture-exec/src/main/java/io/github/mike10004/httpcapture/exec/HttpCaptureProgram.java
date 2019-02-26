package io.github.mike10004.httpcapture.exec;

import com.github.mike10004.nativehelper.subprocess.ProcessMonitor;
import com.github.mike10004.nativehelper.subprocess.ScopedProcessTracker;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.net.HostAndPort;
import com.opencsv.CSVReader;
import io.github.mike10004.httpcapture.AutoCertificateAndKeySource;
import io.github.mike10004.httpcapture.BasicCaptureServer;
import io.github.mike10004.httpcapture.CaptureServer;
import io.github.mike10004.httpcapture.CaptureServerControl;
import io.github.mike10004.httpcapture.HarCaptureMonitor;
import io.github.mike10004.httpcapture.ImmutableHttpRequest;
import io.github.mike10004.httpcapture.ImmutableHttpResponse;
import io.github.mike10004.httpcapture.explode.HarExporter;
import net.lightbody.bmp.core.har.Har;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.Objects.requireNonNull;

class HttpCaptureProgram {

    private static final Logger log = LoggerFactory.getLogger(HttpCaptureProgram.class);

    private final HttpCaptureConfig config;

    public HttpCaptureProgram(HttpCaptureConfig config) {
        this.config = requireNonNull(config);
    }

    public int execute() throws IOException, InterruptedException {
        if (config.explode && config.explodeInput != null) {
            return export(config.explodeInput, config.outputParent);
        }
        return serve();
    }

    public int serve() throws IOException, InterruptedException {
        BasicCaptureServer.Builder builder = BasicCaptureServer.builder();
        Path tempdir = java.nio.file.Files.createTempDirectory(config.tempdirParent, "http-capture-trust");
        builder.collectHttps(new AutoCertificateAndKeySource(tempdir));
        CaptureServer server = builder.build();
        HarCaptureMonitor monitor = createMonitor();
        CaptureServerControl ctrl = server.start(monitor, config.port);
        serverStarted(ctrl);
        OutputSink outputSink = OutputSink.toFileInParent(config.outputParent, config.charset);
        SigtermHook hook = new SigtermHook(ctrl, monitor, outputSink);
        hook.addPostCompletionAction(makeDeleteDirAction(tempdir));
        getRuntime().addShutdownHook(new Thread(hook.asRunnable()));
        config.stderr.format("http-capture: ready; listening on port %d%n", ctrl.getPort());
        if (config.browser != null) {
            Iterable<String> browserArgs = tokenize(config.browserArgs);
            //noinspection unused // TODO: provide an alternate method to initate orderly shutdown using this monitor
            BrowserProcessTracker processTracker = new BrowserProcessTracker();
            hook.addPostCompletionAction(processTracker.createCleanupAction());
            ProcessMonitor<?, ?> processMon = config.browser.getSupport(config)
                    .prepare(tempdir)
                    .launch(getServerAddress(ctrl), browserArgs, processTracker);
            // TODO optionally stop server when browser process terminates
            log.debug("{}", processMon);
        }
        waitForSignal();
        return 0;
    }

    private class BrowserProcessTracker extends ScopedProcessTracker {

        public Runnable createCleanupAction() {
            return () -> {
                if (!config.keepBrowserOpen) {
                    List<?> remaining = BrowserProcessTracker.this.destroyAll();
                    if (!remaining.isEmpty()) {
                        log.warn("{} processes remaining even though we tried to kill them", remaining.size());
                    }
                }
            };
        }
    }

    private HostAndPort getServerAddress(CaptureServerControl ctrl) {
        return HostAndPort.fromParts("127.0.0.1", ctrl.getPort());
    }

    public int export(String pathname, Path destination) throws IOException {
        if (pathname.isEmpty()) {
            config.stderr.println("http-capture: input HAR pathname must be nonempty");
            return 1;
        }
        File inputFile;
        try {
            inputFile = new File(pathname);
        } catch (IllegalArgumentException e) {
            config.stderr.println("http-capture: invalid input file pathname");
            return 1;
        }
        HarExporter exploder = new HarExporter();
        CharSource harSource = com.google.common.io.Files.asCharSource(inputFile, config.charset);
        exploder.export(harSource, destination);
        return 0;
    }

    private class CustomCaptureMonitor extends HarCaptureMonitor {
        @Override
        public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
            if (config.interceptMode.isReport()) {
                config.stderr.format("%d %s %s%n", httpResponse.status, httpRequest.method, httpRequest.url);
            }
        }
    }

    protected HarCaptureMonitor createMonitor() {
        return new CustomCaptureMonitor();
    }

    @VisibleForTesting
    protected void serverStarted(CaptureServerControl ctrl) {

    }

    protected void waitForSignal() throws InterruptedException {
        new CountDownLatch(1).await();
    }

    private static Runnable makeDeleteDirAction(Path directory) {
        return () -> {
            try {
                FileUtils.deleteDirectory(directory.toFile());
            } catch (IOException e) {
                if (directory.toFile().exists()) {
                    log.warn("failed to delete directory {}", directory);
                }
            }
        };
    }

    /**
     * Thin representation of a runtime.
     */
    interface RuntimeFacade {

        /**
         * Adds a shutdown hook. In a facade of the actual runtime, this invokes
         * {@link Runtime#addShutdownHook(Thread)}.
         * @param thread the hook thread
         */
        void addShutdownHook(Thread thread);

        /**
         * Returns a wrapper around the actual runtime.
         * @return a runtime wrapper
         * @see Runtime#getRuntime()
         */
        static RuntimeFacade actual() {
            Runtime rt = Runtime.getRuntime();
            return new RuntimeFacade() {
                @Override
                public void addShutdownHook(Thread thread) {
                    rt.addShutdownHook(thread);
                }
            };
        }
    }

    protected RuntimeFacade getRuntime() {
        return RuntimeFacade.actual();
    }

    /**
     * Class that represents the completion action performed when the user sends the
     * termination signal to the process. In the most common use case
     */
    class SigtermHook {

        private CaptureServerControl serverControl;

        private HarCaptureMonitor monitor;

        private OutputSink outputSink;

        private final List<Runnable> postCompletionActions;

        public SigtermHook(CaptureServerControl serverControl, HarCaptureMonitor monitor, OutputSink outputSink) {
            this.serverControl = serverControl;
            this.monitor = monitor;
            this.outputSink = outputSink;
            this.postCompletionActions = Collections.synchronizedList(new ArrayList<>());
        }

        public Runnable asRunnable() {
            return this::complete;
        }

        public void complete() {
            try {
                if (serverControl.isStarted()) {
                    serverControl.close();
                    if (config.captureMode.isWriteHar()) {
                        Har har = monitor.getCapturedHar();
                        config.stderr.format("http-capture: writing %d HTTP interactions to %s%n", har.getLog().getEntries().size(), outputSink.describe());
                        try (Writer out = outputSink.openStream(har)) {
                            har.writeTo(out);
                        }
                        config.stderr.format("http-capture: wrote %s%n", outputSink.describe());
                        if (config.explode) {
                            File harFile = outputSink.mostRecentFile();
                            if (harFile != null) {
                                String subdirName = FilenameUtils.getBaseName(harFile.getAbsolutePath());
                                File exportDir = config.outputParent.resolve(subdirName).toFile();
                                //noinspection ResultOfMethodCallIgnored
                                exportDir.mkdirs();
                                export(harFile.getAbsolutePath(), exportDir.toPath());
                            } else {
                                log.error("output sink did not create a file");
                            }
                        }
                    }
                } else {
                    config.stderr.println("http-capture: server never started; not writing output file");
                }
            } catch (Exception e) {
                config.stderr.println("http-capture: error during completion phase");
                e.printStackTrace(config.stderr);
            } finally {
                new ArrayList<>(postCompletionActions).forEach(action -> {
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.warn("error during post-capture action", e);
                    }
                });
            }
        }

        public void addPostCompletionAction(Runnable r) {
            requireNonNull(r, "action must be non-null");
            postCompletionActions.add(r);
        }

    }

    protected Iterable<String> tokenize(@Nullable String value) {
        if (value == null) {
            return ImmutableList.of();
        }
        List<String> args = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(value))) {
            List<String[]> rows = reader.readAll();
            rows.forEach(row -> args.addAll(Arrays.asList(row)));
        } catch (IOException e) {
            log.warn("failed to tokenize arguments from " + value, e);
        }
        return args;
    }

}
