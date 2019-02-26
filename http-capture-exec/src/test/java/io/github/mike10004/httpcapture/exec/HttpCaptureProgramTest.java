package io.github.mike10004.httpcapture.exec;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class HttpCaptureProgramTest {

    @Test
    public void timestamp() throws Exception {
        String stamp = new UnitTestProgram().timestamp();
        assertTrue("stamp in correct format", stamp.matches("\\d{8}T\\d{6}"));
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

        public UnitTestProgram() throws UnsupportedEncodingException {
            this(new StreamBucket(), new StreamBucket());
        }

        private UnitTestProgram(StreamBucket stdout, StreamBucket stderr) {
            super(stdout, stderr);
            this.stdoutBucket = stdout;
            this.stderrBucket = stderr;
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