package io.github.mike10004.httpcapture.exec;

import com.google.common.io.Files;
import com.google.common.net.HostAndPort;
import io.github.mike10004.httpcapture.CaptureServerControl;
import io.github.mike10004.httpcapture.testing.HarTestCase;
import io.github.mike10004.httpcapture.testing.StreamBucket;
import io.github.mike10004.httpcapture.testing.TestClients;
import org.apache.http.client.methods.HttpGet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpCaptureProgramTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void execute_serve() throws Exception {
        AtomicInteger portHolder = new AtomicInteger();
        CountDownLatch portReceivedLatch = new CountDownLatch(1);
        UnitTestProgram program = new UnitTestProgram() {
            @Override
            protected void serverStarted(CaptureServerControl ctrl) {
                portHolder.set(ctrl.getPort());
                portReceivedLatch.countDown();
            }
        };
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Integer> executeFuture = executorService.submit(program::execute);
        assertTrue("port received before timeout", portReceivedLatch.await(10, TimeUnit.SECONDS));
        HostAndPort serverAddress = HostAndPort.fromParts("127.0.0.1", portHolder.get());
        TestClients.fetchTextWithBlindTrust(serverAddress, new HttpGet("https://www.microsoft.com/"));
        // pretend SIGTERM was sent; execute shutdown hooks
        program.rt.threadsAdded.forEach(thread -> {
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        });
        program.sigtermLatch.countDown();
        int exitCode = executeFuture.get(10, TimeUnit.SECONDS);
        assertEquals("exit code", 0, exitCode);
        File outputFile = java.nio.file.Files.walk(program.config.outputParent)
                .map(Path::toFile)
                .filter(File::isFile)
                .findFirst().orElseThrow(() -> new AssertionError("no files in " + program.config.outputParent));
        String json = Files.asCharSource(outputFile, program.config.charset).read();
        assertTrue("json contains URL", json.contains("www.microsoft.com"));
    }

    private static class FakeRuntime implements HttpCaptureProgram.RuntimeFacade {

        public List<Thread> threadsAdded = new CopyOnWriteArrayList<>();

        @Override
        public void addShutdownHook(Thread thread) {
            threadsAdded.add(thread);
        }
    }

    private HttpCaptureConfig parameterizeConfig(HttpCaptureConfig config, PrintStream stdout, PrintStream stderr) throws IOException {
        config.stdout = stdout;
        config.stderr = stderr;
        config.outputParent = temporaryFolder.newFolder().toPath();
        return config;
    }

    private class UnitTestProgram extends HttpCaptureProgram {

        public final FakeRuntime rt = new FakeRuntime();
        public final CountDownLatch sigtermLatch = new CountDownLatch(1);
        public final StreamBucket stdoutBucket, stderrBucket;
        public final HttpCaptureConfig config;

        public UnitTestProgram() throws IOException {
            this(new StreamBucket(), new StreamBucket());
        }

        private UnitTestProgram(StreamBucket stdout, StreamBucket stderr) throws IOException {
            this(parameterizeConfig(new HttpCaptureConfig(), stdout, stderr));
        }

        public UnitTestProgram(HttpCaptureConfig config) {
            super(config);
            this.stdoutBucket = (StreamBucket) config.stdout;
            this.stderrBucket = (StreamBucket) config.stderr;
            this.config = config;
        }

        @Override
        protected RuntimeFacade getRuntime() {
            return rt;
        }

        @Override
        protected void waitForSignal() throws InterruptedException {
            sigtermLatch.await();
        }

    }

    @Test
    public void execute_export() throws Exception {
        HarTestCase testCase = new HarTestCase("/example-captured.har");
        File harFile = testCase.getPathname(temporaryFolder.getRoot().toPath());
        HttpCaptureConfig config = new HttpCaptureConfig();
        config.stdout = new StreamBucket();
        config.stderr = new StreamBucket();
        config.export = true;
        config.exportInputPathname = harFile.getAbsolutePath();
        config.outputParent = temporaryFolder.newFolder().toPath();
        UnitTestProgram program = new UnitTestProgram(config);
        int exitCode = program.execute();
        assertEquals("exit code", 0, exitCode);
        testCase.checkExport(config.outputParent);
    }
}