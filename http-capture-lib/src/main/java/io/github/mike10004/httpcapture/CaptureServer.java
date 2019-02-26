package io.github.mike10004.httpcapture;

import javax.annotation.Nullable;
import java.io.IOException;

public interface CaptureServer {

    CaptureServerControl start(CaptureMonitor monitor, @Nullable Integer port) throws IOException;

    default CaptureServerControl start(CaptureMonitor monitor) throws IOException {
        return start(monitor, null);
    }

}
