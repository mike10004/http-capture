package io.github.mike10004.httpcapture;

import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.ChainedProxyType;

import java.net.InetSocketAddress;
import java.util.Queue;

/**
 * @author https://gist.github.com/jbaldassari/a13f9032999e82711a282d0c7a4b452c
 */
class UpstreamProxy implements ChainedProxyManager {

    private final ChainedProxyType proxyType;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public UpstreamProxy(final ChainedProxyType proxyType, final String host, final int port,
                  final String username, final String password) {
        this.proxyType = proxyType;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public void lookupChainedProxies(final HttpRequest httpRequest,
                                     final Queue<ChainedProxy> chainedProxies) {
        chainedProxies.add(new ChainedProxyAdapter() {
            @Override
            public ChainedProxyType getChainedProxyType() {
                return proxyType;
            }
            @Override
            public InetSocketAddress getChainedProxyAddress() {
                return new InetSocketAddress(host, port);
            }
            @Override
            public String getUsername() {
                return username;
            }
            @Override
            public String getPassword() {
                return password;
            }
        });
    }

    @Override
    public String toString() {
        return proxyType.toString().toLowerCase() + "://" + host + ":" + port;
    }
}
