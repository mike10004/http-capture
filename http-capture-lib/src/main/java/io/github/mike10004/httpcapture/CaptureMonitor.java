package io.github.mike10004.httpcapture;

import io.netty.handler.codec.http.HttpObject;
import net.lightbody.bmp.core.har.Har;
import org.littleshoot.proxy.HttpFiltersSource;

/**
 * Interface for classes that is notified of HTTP requests and responses
 * generated during a capture session.
 */
public interface CaptureMonitor {

    /**
     * Callback invoked when a response from the remote server is received by the capturing proxy.
     * This method is invoked from {@link CaptureMonitorFilter#serverToProxyResponse(HttpObject)}
     * or one of that class's error methods.
     * @param httpResponse the HTTP response
     * @param httpRequest the HTTP request
     * @see CaptureMonitorFilter
     */
    default void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {

    }

    void harCaptured(Har har);


    /**
     * Gets teh max request buffer size.
     * @return max request buffer size in bytes
     * @see HttpFiltersSource#getMaximumRequestBufferSizeInBytes()
     */
    default int getMaximumRequestBufferSizeInBytes() {
        return 0;
    }

    /**
     * Gets the max response buffer size.
     * @return max response buffer size in bytes
     * @see HttpFiltersSource#getMaximumResponseBufferSizeInBytes()
     */
    default int getMaximumResponseBufferSizeInBytes() {
        return 0;
    }

}
