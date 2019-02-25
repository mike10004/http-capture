package io.github.mike10004.httpcapture;

import net.lightbody.bmp.core.har.Har;

public class HarCaptureMonitor implements CaptureMonitor {

    private Har har;

    public HarCaptureMonitor() {
    }

    @Override
    public void harCaptured(Har har) {
        this.har = har;
    }

    public Har getCapturedHar() {
        return har;
    }
}
