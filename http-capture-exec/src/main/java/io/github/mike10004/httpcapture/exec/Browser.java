package io.github.mike10004.httpcapture.exec;

/**
 * Enumeration of supported browsers.
 * Must be public for joptsimple to parse.
 */
public enum Browser {

    chrome, chromium;

    BrowserSupport getSupport(HttpCaptureConfig config) {
        switch (this) {
            case chrome:
                return new ChromeBrowserSupport("google-chrome", determineOutputDestination(config));
            case chromium:
                return new ChromeBrowserSupport("chromium-browser", determineOutputDestination(config));
        }
        throw new IllegalStateException("not supported: " + this);
    }

    private ChromeBrowserSupport.OutputDestination determineOutputDestination(HttpCaptureConfig config) {
        return config.echoBrowserOutput ? ChromeBrowserSupport.OutputDestination.CONSOLE : ChromeBrowserSupport.OutputDestination.FILES;
    }
}
