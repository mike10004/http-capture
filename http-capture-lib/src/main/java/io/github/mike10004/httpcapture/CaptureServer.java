package io.github.mike10004.httpcapture;

import javax.annotation.Nullable;
import java.io.IOException;

public interface CaptureServer {

    CaptureServerControl start(@Nullable CaptureMonitor monitor, @Nullable Integer port) throws IOException;

    default CaptureServerControl start(@Nullable CaptureMonitor monitor) throws IOException {
        return start(monitor, null);
    }

}
