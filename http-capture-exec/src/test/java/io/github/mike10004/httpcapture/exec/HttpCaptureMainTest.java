package io.github.mike10004.httpcapture.exec;

import io.github.mike10004.httpcapture.testing.StreamBucket;
import org.junit.Test;

import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HttpCaptureMainTest {

    @Test
    public void main0_help() throws Exception {
        StreamBucket stdoutBucket = new StreamBucket();
        HttpCaptureMain main = new HttpCaptureMain() {
            @Override
            protected PrintStream stdout() {
                return stdoutBucket;
            }
        };
        int exitCode = main.main0(new String[]{"--help"});
        assertEquals("clean exit", 0, exitCode);
        String content = stdoutBucket.dump();
        assertFalse("prints something", content.trim().isEmpty());
    }

}