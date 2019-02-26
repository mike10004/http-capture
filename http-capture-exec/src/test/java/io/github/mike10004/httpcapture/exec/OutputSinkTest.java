package io.github.mike10004.httpcapture.exec;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class OutputSinkTest {

    public static class FileOutputSinkTest {

        @Test
        public void timestamp() throws Exception {
            OutputSink.FileOutputSink sink = new OutputSink.FileOutputSink(FileUtils.getUserDirectory().toPath(), StandardCharsets.UTF_8);
            String stamp = sink.timestamp();
            assertTrue("stamp in correct format", stamp.matches("\\d{8}T\\d{6}"));
        }
    }

}