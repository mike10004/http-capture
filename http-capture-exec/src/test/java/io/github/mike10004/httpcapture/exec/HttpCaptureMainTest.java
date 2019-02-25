package io.github.mike10004.httpcapture.exec;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class HttpCaptureMainTest {

    @org.junit.Test
    public void main0() {
        fail("not yet implemented");
    }

    @org.junit.Test
    public void timestamp() {
        String stamp = new HttpCaptureMain().timestamp();
        assertTrue("stamp in correct format", stamp.matches("\\d{8}T\\d{6}"));
    }

    private static class FakeRuntime implements HttpCaptureMain.RuntimeFacade {

        public List<Thread> threadsAdded = new CopyOnWriteArrayList<>();

        @Override
        public void addShutdownHook(Thread thread) {
            threadsAdded.add(thread);
        }
    }

    private static class UnitTestMain extends HttpCaptureMain {

        public final FakeRuntime rt = new FakeRuntime();
        public final List<CountDownLatch> latches = new CopyOnWriteArrayList<>();

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