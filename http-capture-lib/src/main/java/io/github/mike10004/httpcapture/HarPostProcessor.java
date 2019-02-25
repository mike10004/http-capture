package io.github.mike10004.httpcapture;

import net.lightbody.bmp.core.har.Har;

/**
 * Interface for classes that apply modifications to HARs after traffic collection.
 */
public interface HarPostProcessor {
    void process(Har har);
}
