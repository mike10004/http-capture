package io.github.mike10004.httpcapture;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.CaptureType;
import org.littleshoot.proxy.ChainedProxyType;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.impl.ProxyUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a traffic collector. Warning: this may become package-private or be renamed in a future release.
 */
public class BasicCaptureServer implements CaptureServer {

    @Nullable
    private final CertificateAndKeySource certificateAndKeySource;
    private final ImmutableList<HttpFiltersSource> httpFiltersSources;
    private final BmpConfigurator upstreamConfigurator;
    private final Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator;
    private final ImmutableList<HarPostProcessor> harPostProcessors;

    /**
     * Constructs an instance of the class. Should only be used by subclasses that know
     * what they're doing. Otherwise, use {@link TrafficCollector#builder(WebDriverFactory)} to create
     * an instance.
     * @param webDriverFactory web driver factory to use
     * @param certificateAndKeySource credential source
     * @param upstreamConfigurator upstream proxy configurator
     * @param httpFiltersSources list of filters sources; this should probably include {@link AnonymizingFiltersSource}
     * @param interceptingProxyInstantiator supplier that constructs the local proxy instance
     * @param harPostProcessors list of HAR post-processors
     * @param exceptionReactor exception reactor
     */
    protected BasicCaptureServer(@Nullable CertificateAndKeySource certificateAndKeySource,
                                 BmpConfigurator upstreamConfigurator,
                                 Iterable<? extends HttpFiltersSource> httpFiltersSources,
                                 Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator,
                                 Iterable<? extends HarPostProcessor> harPostProcessors) {
        this.certificateAndKeySource = certificateAndKeySource;
        this.httpFiltersSources = ImmutableList.copyOf(httpFiltersSources);
        this.upstreamConfigurator = requireNonNull(upstreamConfigurator);
        this.interceptingProxyInstantiator = requireNonNull(interceptingProxyInstantiator);
        this.harPostProcessors = ImmutableList.copyOf(harPostProcessors);
    }

    protected Set<CaptureType> getCaptureTypes() {
        return EnumSet.allOf(CaptureType.class);
    }

    private abstract class BasicControl implements CaptureServerControl {

        private final int port;

        public BasicControl(int port) {
            this.port = port;
        }

        @Override
        public int getPort() {
            return port;
        }

    }

    @Override
    public CaptureServerControl start(@Nullable CaptureMonitor monitor, @Nullable Integer port) throws IOException {
        BrowserMobProxy bmp = instantiateProxy();
        configureProxy(bmp, certificateAndKeySource, monitor);
        bmp.enableHarCaptureTypes(getCaptureTypes());
        bmp.newHar();
        if (port == null) {
            bmp.start();
        } else {
            bmp.start(port);
        }
        return new BasicControl(bmp.getPort()) {
            @Override
            public void close() {
                bmp.stop();
                if (monitor != null) {
                    Har har = bmp.getHar();
                    for (HarPostProcessor harPostProcessor : harPostProcessors) {
                        harPostProcessor.process(har);
                    }
                    monitor.harCaptured(har);
                }
            }
        };
    }

    private class MonitorFiltersSource extends HttpFiltersSourceAdapter {

        private final CaptureMonitor monitor;

        private MonitorFiltersSource(CaptureMonitor monitor) {
            this.monitor = requireNonNull(monitor);
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest) {
            return doFilterRequest(originalRequest, null);
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return doFilterRequest(originalRequest, ctx);
        }

        private HttpFilters doFilterRequest(HttpRequest originalRequest, @Nullable ChannelHandlerContext ctx) {
            if (!ProxyUtils.isCONNECT(originalRequest)) {
                return new TrafficMonitorFilter(originalRequest, ctx, monitor);
            } else {
                return null;
            }
        }

        @Override
        public int getMaximumRequestBufferSizeInBytes() {
            return monitor.getMaximumRequestBufferSizeInBytes();
        }

        @Override
        public int getMaximumResponseBufferSizeInBytes() {
            return monitor.getMaximumResponseBufferSizeInBytes();
        }
    }

    protected MitmManager createMitmManager(@SuppressWarnings("unused") BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) {
        MitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(certificateAndKeySource)
                .build();
        return mitmManager;
    }

    protected BrowserMobProxy instantiateProxy() {
        return interceptingProxyInstantiator.get();
    }

    protected void configureProxy(BrowserMobProxy bmp, CertificateAndKeySource certificateAndKeySource, @Nullable CaptureMonitor trafficMonitor) {
        if (certificateAndKeySource != null) {
            MitmManager mitmManager = createMitmManager(bmp, certificateAndKeySource);
            bmp.setMitmManager(mitmManager);
        }
        if (trafficMonitor != null) {
            bmp.addLastHttpFilterFactory(new MonitorFiltersSource(trafficMonitor));
        }
        httpFiltersSources.forEach(bmp::addLastHttpFilterFactory);
        upstreamConfigurator.configureUpstream(bmp);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        if (certificateAndKeySource != null) h.add("certificateAndKeySource", certificateAndKeySource);
        if (httpFiltersSources != null) h.add("httpFiltersSources", httpFiltersSources);
        if (upstreamConfigurator != null) h.add("upstreamConfigurator", upstreamConfigurator);
        if (interceptingProxyInstantiator != null) {
            h.add("interceptingProxyInstantiator", interceptingProxyInstantiator);
        }
        if (harPostProcessors != null) h.add("harPostProcessors.size", harPostProcessors.size());
        return h.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public static final class Builder {

        private CertificateAndKeySource certificateAndKeySource = null;
        private final List<HttpFiltersSource> httpFiltersSources = new ArrayList<>();
        private BmpConfigurator upstreamConfigurator = BmpConfigurator.inoperative();
        private Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator = BrAwareBrowserMobProxyServer::new;
        private final List<HarPostProcessor> harPostProcessors = new ArrayList<>();

        Builder() {
            httpFiltersSources.add(AnonymizingFiltersSource.getInstance());
        }

        public Builder collectHttps(CertificateAndKeySource certificateAndKeySource) {
            this.certificateAndKeySource = requireNonNull(certificateAndKeySource);
            return this;
        }

        /**
         * Sets the supplier of the proxy server instance that is used to intercept and collect traffic.
         * By default, we supply a custom implementation that supports brotli decoding,
         * {@link BrAwareBrowserMobProxyServer}. To revert this behavior to a more hands-off implementation,
         * set this to a supplier of a {@link net.lightbody.bmp.BrowserMobProxyServer} instance.
         * @param interceptingProxyInstantiator the instantiator
         * @return this builder instance
         */
        public Builder interceptingProxyInstantiator(Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator) {
            this.interceptingProxyInstantiator = requireNonNull(interceptingProxyInstantiator);
            return this;
        }

        public Builder nonAnonymizing() {
            httpFiltersSources.remove(AnonymizingFiltersSource.getInstance());
            return this;
        }

        public Builder filter(HttpFiltersSource filter) {
            httpFiltersSources.add(filter);
            return this;
        }

        /**
         * Adds all argument filters sources to this builder's filters list.
         * @param val the filters sources to add
         * @return this instance
         */
        public Builder filters(Collection<? extends HttpFiltersSource> val) {
            httpFiltersSources.addAll(val);
            return this;
        }

        public Builder noUpstreamProxy() {
            return upstreamProxy(BmpConfigurator.noProxy());
        }

        private Builder upstreamProxy(BmpConfigurator configurator) {
            this.upstreamConfigurator = requireNonNull(configurator);
            return this;
        }

        @Deprecated
        public Builder upstreamProxy(InetSocketAddress address, ChainedProxyType proxyType) {
            if (address == null) {
                return noUpstreamProxy();
            } else {
                return upstreamProxy(() -> literalize(address), proxyType);
            }
        }

        private static HostAndPort literalize(InetSocketAddress socketAddress) {
            if (socketAddress == null) {
                return null;
            }
            return HostAndPort.fromParts(socketAddress.getHostString(), socketAddress.getPort());
        }

        private Builder upstreamProxy(Supplier<HostAndPort> supplier, ChainedProxyType proxyType) {
            requireNonNull(proxyType);
            return upstreamProxy(() -> {
                HostAndPort socketAddress = supplier.get();
                if (socketAddress == null) {
                    return null;
                }
                return ProxyUris.createSimple(socketAddress, proxyType);
            });
        }

        /**
         * Configures the collector to use an upstream proxy specified by a URI. The URI components
         * must be as described in {@link WebdrivingConfig#getProxySpecification()}.
         * @param proxySpecificationSupplier
         * @return this builder instance
         */
        public Builder upstreamProxy(Supplier<URI> proxySpecificationSupplier) {
            this.upstreamConfigurator = BmpConfigurator.upstream(proxySpecificationSupplier);
            return this;
        }

        public Builder harPostProcessor(HarPostProcessor harPostProcessor) {
            harPostProcessors.add(harPostProcessor);
            return this;
        }

        public BasicCaptureServer build() {
            return new BasicCaptureServer(
                    certificateAndKeySource, upstreamConfigurator,
                    httpFiltersSources, interceptingProxyInstantiator,
                    harPostProcessors);
        }

    }

}
