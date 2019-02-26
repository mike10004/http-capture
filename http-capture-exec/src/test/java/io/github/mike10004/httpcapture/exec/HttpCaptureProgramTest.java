package io.github.mike10004.httpcapture.exec;

import com.google.common.io.Files;
import com.google.common.net.HostAndPort;
import io.github.mike10004.httpcapture.CaptureServerControl;
import io.github.mike10004.httpcapture.testing.StreamBucket;
import io.github.mike10004.httpcapture.testing.TestClients;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

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

import static org.junit.Assert.*;

public class HttpCaptureProgramTest {

    @Test
    public void execute() throws Exception {
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
        program.latches.forEach(CountDownLatch::countDown);
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

    private static class UnitTestProgram extends HttpCaptureProgram {

        public final FakeRuntime rt = new FakeRuntime();
        public final List<CountDownLatch> latches = new CopyOnWriteArrayList<>();
        public final StreamBucket stdoutBucket, stderrBucket;
        public final HttpCaptureConfig config;

        public UnitTestProgram() throws IOException {
            this(new StreamBucket(), new StreamBucket());
        }

        private UnitTestProgram(StreamBucket stdout, StreamBucket stderr) throws IOException {
            this(stdout, stderr, makeConfig(stdout, stderr));
        }

        private UnitTestProgram(StreamBucket stdout, StreamBucket stderr, HttpCaptureConfig config) {
            super(config);
            this.stdoutBucket = stdout;
            this.stderrBucket = stderr;
            this.config = config;
        }

        private static HttpCaptureConfig makeConfig(PrintStream stdout, PrintStream stderr) throws IOException {
            HttpCaptureConfig config = new HttpCaptureConfig();
            config.stdout = stdout;
            config.stderr = stderr;
            config.outputParent = java.nio.file.Files.createTempDirectory(FileUtils.getTempDirectory().toPath(), "unit-test-output");
            return config;
        }

        @Override
        protected RuntimeFacade getRuntime() {
            return rt;
        }

        @Override
        protected CountDownLatch makeLatch() {
            CountDownLatch latch = super.makeLatch();
            latches.add(latch);
            return latch;
        }
    }

}