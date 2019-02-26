package io.github.mike10004.httpcapture.exec;

import com.google.common.annotations.VisibleForTesting;
import io.github.mike10004.httpcapture.AutoCertificateAndKeySource;
import io.github.mike10004.httpcapture.BasicCaptureServer;
import io.github.mike10004.httpcapture.CaptureServer;
import io.github.mike10004.httpcapture.CaptureServerControl;
import io.github.mike10004.httpcapture.HarCaptureMonitor;
import io.github.mike10004.httpcapture.ImmutableHttpRequest;
import io.github.mike10004.httpcapture.ImmutableHttpResponse;
import net.lightbody.bmp.core.har.Har;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
        BasicCaptureServer.Builder builder = BasicCaptureServer.builder();
        Path tempdir = java.nio.file.Files.createTempDirectory(config.tempdirParent, "http-capture-trust");
        builder.collectHttps(new AutoCertificateAndKeySource(tempdir));
        CaptureServer server = builder.build();
        HarCaptureMonitor monitor = new HarCaptureMonitor() {
            @Override
            public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
            if (!config.suppressInteractionEcho) {
                config.stderr.format("%d %s %s%n", httpResponse.status, httpRequest.method, httpRequest.url);
            }
            }
        };
        CaptureServerControl ctrl = server.start(monitor, config.port);
        serverStarted(ctrl);
        OutputSink outputSink = OutputSink.toFileInParent(config.parentDir, config.charset);
        List<Runnable> postCompletionActions = new ArrayList<>();
        postCompletionActions.add(makeDeleteDirAction(tempdir));
        SigtermHook hook = new SigtermHook(ctrl, monitor, outputSink, postCompletionActions);
        getRuntime().addShutdownHook(new Thread(hook.asRunnable()));
        config.stderr.format("http-capture: ready; listening on port %d%n", ctrl.getPort());
        waitForSignal();
        return 0;
    }

    @VisibleForTesting
    protected void serverStarted(CaptureServerControl ctrl) {

    }

    protected void waitForSignal() throws InterruptedException {
        makeLatch().await();
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

    protected CountDownLatch makeLatch() {
        return new CountDownLatch(1);
    }

    interface RuntimeFacade {
        void addShutdownHook(Thread thread);
    }

    protected RuntimeFacade getRuntime() {
        Runtime rt = Runtime.getRuntime();
        return new RuntimeFacade() {
            @Override
            public void addShutdownHook(Thread thread) {
                rt.addShutdownHook(thread);
            }
        };
    }

    class SigtermHook {

        private CaptureServerControl serverControl;
        private HarCaptureMonitor monitor;
        private OutputSink outputSink;
        private final List<Runnable> postCompletionActions;

        public SigtermHook(CaptureServerControl serverControl, HarCaptureMonitor monitor, OutputSink outputSink, Collection<Runnable> postCompletionActions) {
            this.serverControl = serverControl;
            this.monitor = monitor;
            this.outputSink = outputSink;
            this.postCompletionActions = Collections.unmodifiableList(new ArrayList<>(postCompletionActions));
        }

        public Runnable asRunnable() {
            return this::complete;
        }

        public void complete() {
            try {
                if (serverControl.isStarted()) {
                    serverControl.close();
                    Har har = monitor.getCapturedHar();
                    config.stderr.format("http-capture: writing %d HTTP interactions to %s%n", har.getLog().getEntries().size(), outputSink.describe());
                    try (Writer out = outputSink.openStream(har)) {
                        har.writeTo(out);
                    }
                    config.stderr.format("http-capture: wrote %s%n", outputSink.describe());
                } else {
                    config.stderr.println("http-capture: server never started; not writing output file");
                }
            } catch (Exception e) {
                config.stderr.println("http-capture: error during completion phase");
                e.printStackTrace(config.stderr);
            } finally {
                postCompletionActions.forEach(action -> {
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.warn("error during post-capture action", e);
                    }
                });
            }
        }

    }

}
