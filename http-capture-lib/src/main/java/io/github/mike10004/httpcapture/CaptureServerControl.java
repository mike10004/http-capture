package io.github.mike10004.httpcapture;

public interface CaptureServerControl extends java.io.Closeable {

    int getPort();

    boolean isStarted();

}
