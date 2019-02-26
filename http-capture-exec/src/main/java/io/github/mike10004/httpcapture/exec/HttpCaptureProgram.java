package io.github.mike10004.httpcapture.exec;

import com.google.common.io.CharSink;
import com.google.common.io.Files;
import io.github.mike10004.httpcapture.AutoCertificateAndKeySource;
import io.github.mike10004.httpcapture.BasicCaptureServer;
import io.github.mike10004.httpcapture.CaptureServer;
import io.github.mike10004.httpcapture.CaptureServerControl;
import io.github.mike10004.httpcapture.HarCaptureMonitor;
import io.github.mike10004.httpcapture.ImmutableHttpRequest;
import io.github.mike10004.httpcapture.ImmutableHttpResponse;
import net.lightbody.bmp.core.har.Har;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

class HttpCaptureProgram {

    private final PrintStream stdout, stderr;

    public HttpCaptureProgram(PrintStream stdout, PrintStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int execute(HttpCaptureConfig config) throws Exception {
        BasicCaptureServer.Builder builder = BasicCaptureServer.builder();
        Path tempdir = java.nio.file.Files.createTempDirectory(config.tempdirParent, "http-capture-trust");
        builder.collectHttps(new AutoCertificateAndKeySource(tempdir));
        // configure per options
        CaptureServer server = builder.build();
        HarCaptureMonitor monitor = new HarCaptureMonitor() {
            @Override
            public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
            if (!config.suppressInteractionEcho) {
                stderr.format("%d %s %s%n", httpResponse.status, httpRequest.method, httpRequest.url);
            }
            }
        };
        CaptureServerControl ctrl = server.start(monitor, config.port);
        //noinspection ResultOfMethodCallIgnored
        config.parentDir.toFile().mkdirs();
        File outputFile = config.parentDir.resolve(String.format("http-capture-%s.har", timestamp())).toFile();
        CharSink outputSink = Files.asCharSink(outputFile, config.charset);
        String outputSinkDescription = outputFile.getAbsolutePath();
        SigtermHook hook = new SigtermHook(ctrl, monitor, outputSink, outputSinkDescription);
        hook.onCompletion(() -> {
            try {
                FileUtils.deleteDirectory(tempdir.toFile());
            } catch (IOException e) {
                e.printStackTrace(stderr);
            }
        });
        getRuntime().addShutdownHook(new Thread(hook.asRunnable()));
        stderr.format("http-capture: listening on port %d%n", ctrl.getPort());
        makeLatch().await();
        return 0;
    }

    protected String timestamp() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date.from(Instant.now()));
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

    private class SigtermHook {

        private CaptureServerControl serverControl;
        private HarCaptureMonitor monitor;
        private CharSink outputSink;
        private String outputSinkDescription;
        private final List<Runnable> postCompletionActions;

        public SigtermHook(CaptureServerControl serverControl, HarCaptureMonitor monitor, CharSink outputSink, String outputSinkDescription) {
            this.serverControl = serverControl;
            this.monitor = monitor;
            this.outputSink = outputSink;
            this.outputSinkDescription = outputSinkDescription;
            postCompletionActions = new CopyOnWriteArrayList<>();
        }

        public void onCompletion(Runnable action) {
            postCompletionActions.add(action);
        }

        public Runnable asRunnable() {
            return this::complete;
        }

        public void complete() {
            try {
                serverControl.close();
                Har har = monitor.getCapturedHar();
                try (Writer out = outputSink.openStream()) {
                    har.writeTo(out);
                }
                stderr.format("writing %d HTTP interactions to %s%n", har.getLog().getEntries().size(), outputSinkDescription);
                stderr.format("http-capture: wrote har to %s%n", outputSinkDescription);
            } catch (Exception e) {
                e.printStackTrace(stderr);
            } finally {
                postCompletionActions.forEach(action -> {
                    try {
                        action.run();
                    } catch (Exception e) {
                        e.printStackTrace(stderr);
                    }
                });
            }
        }

    }

}