package io.github.mike10004.httpcapture.exec;

import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Value class that represents a configuration for a capture session.
 * <p>TODO move to final/builder pattern for config
 */
class HttpCaptureConfig {

    @Nullable
    public Integer port = null;

    public PrintStream stdout = System.out;

    public PrintStream stderr = System.err;

    public Charset charset = StandardCharsets.UTF_8;

    public Path outputParent = new File(System.getProperty("user.dir")).toPath();

    public Path tempdirParent = FileUtils.getTempDirectory().toPath();

    public InterceptMode interceptMode = InterceptMode.REPORT;

    public CaptureMode captureMode = CaptureMode.WRITE_HAR;

    public enum InterceptMode {

        REPORT, IGNORE;

        public boolean isReport() {
            return this == REPORT;
        }
    }

    public enum CaptureMode {
        WRITE_HAR, DISCARD_HAR;

        public boolean isWriteHar() {
            return this == WRITE_HAR;
        }
    }
}
