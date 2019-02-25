package io.github.mike10004.httpcapture;

import javax.annotation.Nullable;
import java.io.IOException;

public interface CaptureServer {

    CaptureServerControl start(@Nullable CaptureMonitor trafficMonitor) throws IOException;

}
