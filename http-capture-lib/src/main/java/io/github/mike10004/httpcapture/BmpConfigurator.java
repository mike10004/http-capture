package io.github.mike10004.httpcapture;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import org.littleshoot.proxy.ChainedProxyManager;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a service class that performs configuration operations relating to proxy instances.
 */
public interface BmpConfigurator {

    /**
     * Configures the chained proxy manager of a proxy instance.
     * @param proxy the proxy to configure
     */
    void configureUpstream(BrowserMobProxy proxy);

    /**
     * Returns a configurator that configures a direct connection upstream, meaning no proxy is to be used.
     * @return a configurator
     */
    static BmpConfigurator noProxy() {
        return new BmpConfigurator() {
            @Override
            public void configureUpstream(BrowserMobProxy bmp) {
                bmp.setChainedProxy(null);
                if (bmp instanceof BrowserMobProxyServer) {
                    ((BrowserMobProxyServer)bmp).setChainedProxyManager(null);
                }
            }

            @Override
            public String toString() {
                return "BmpConfigurator{UPSTREAM_NOPROXY}";
            }
        };
    }

    /**
     * Returns a configurator that does not act upon a proxy instance.
     * @return a configurator instance
     */
    static BmpConfigurator inoperative() {
        return new BmpConfigurator() {
            @Override
            public void configureUpstream(BrowserMobProxy proxy) {
            }

            @Override
            public String toString() {
                return "BmpConfigurator{INOPERATIVE}";
            }
        };
    }

    /**
     * Returns a configurator that configures a proxy to use an upstream proxy specified by a URI.
     * @param proxySpecUriProvider the supplier of the URI
     * @return a configurator instance
     */
    static BmpConfigurator upstream(Supplier<URI> proxySpecUriProvider) {
        requireNonNull(proxySpecUriProvider);
        return new BmpConfigurator() {
            @Override
            public void configureUpstream(BrowserMobProxy bmp) {
                @Nullable URI proxySpecUri = proxySpecUriProvider.get();
                if (proxySpecUri == null) {
                    noProxy().configureUpstream(bmp);
                } else {
                    ChainedProxyManager chainedProxyManager = ProxyUris.toUpstreamProxy(proxySpecUri);
                    ((BrowserMobProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
                }
            }

        };
    }

}
