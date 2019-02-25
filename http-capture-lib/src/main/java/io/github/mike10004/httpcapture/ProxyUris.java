package io.github.mike10004.httpcapture;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.littleshoot.proxy.ChainedProxyType;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class ProxyUris {

    public static final String PARAM_BYPASS = "bypass";

    private ProxyUris() {}

    public static List<String> getProxyBypassesFromQueryString(@Nullable URI proxySpecification) {
        if (proxySpecification != null) {
            List<NameValuePair> queryParams = URLEncodedUtils.parse(proxySpecification, StandardCharsets.UTF_8);
            return queryParams.stream()
                    .filter(param -> PARAM_BYPASS.equalsIgnoreCase(param.getName()))
                    .map(NameValuePair::getValue)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static String getUsername(URI uri) {
        return getCredentials(uri)[0];
    }

    public static String getPassword(URI uri) {
        return getCredentials(uri)[1];
    }

    private static String[] getCredentials(URI uri) {
        String userInfo = uri.getUserInfo();
        if (!Strings.isNullOrEmpty(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            return parts.length == 2 ? parts : new String[]{parts[0], null};
        }
        return new String[]{null, null};
    }

    @Nullable
    public static UpstreamProxy toUpstreamProxy(URI uri) {
        if (uri == null) {
            return null;
        }
        ChainedProxyType type = ChainedProxyType.HTTP;
        if (isSocks(uri)) {
            @Nullable Integer socksVersion = parseSocksVersionFromUriScheme(uri);
            if (Integer.valueOf(4).equals(socksVersion)) {
                type = ChainedProxyType.SOCKS4;
            } else {
                type = ChainedProxyType.SOCKS5;
            }
        }
        String username = getUsername(uri), password = getPassword(uri);
        return new UpstreamProxy(type, uri.getHost(), uri.getPort(), username, password);
    }

    public static boolean isSocks(URI proxySpecification) {
        String scheme = proxySpecification.getScheme();
        return "socks".equalsIgnoreCase(scheme) || "socks4".equalsIgnoreCase(scheme) || "socks5".equalsIgnoreCase(scheme);
    }

    @Nullable
    public static Integer parseSocksVersionFromUriScheme(URI uri) {
        String uriScheme = uri.getScheme();
        if ("socks4".equalsIgnoreCase(uriScheme)) {
            return 4;
        }
        if ("socks5".equalsIgnoreCase(uriScheme)) {
            return 5;
        }
        return null;
    }

    @Nullable
    public static String toScheme(@Nullable ChainedProxyType upstreamProxyType) {
        if (upstreamProxyType == null) {
            return null;
        }
        return upstreamProxyType.name().toLowerCase();
    }

    @Nullable
    public static URI createSimple(@Nullable HostAndPort socketAddress, ChainedProxyType proxyType) {
        if (socketAddress == null) {
            return null;
        }
        try {
            return new URIBuilder()
                    .setHost(socketAddress.getHost())
                    .setPort(socketAddress.getPort())
                    .setScheme(proxyType.name().toLowerCase())
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructs a URI based on a given URI but with an additional proxy host bypass pattern.
     * @param proxyUri the original proxy URI
     * @param hostPattern the new host bypass pattern
     * @return the new URI
     */
    public static URI addBypass(URI proxyUri, String hostPattern) {
        requireNonNull(hostPattern, "hostPattern");
        return addBypasses(proxyUri, Collections.singleton(hostPattern));
    }

    /**
     * Constructs a URI with host bypass patterns. The new URI is based on the old URI.
     * @param proxyUri the original proxy URI
     * @param bypasses a collection of host bypass patterns
     * @return the new URI
     */
    public static URI addBypasses(URI proxyUri, Collection<String> bypasses) {
        if (bypasses.isEmpty()) {
            return proxyUri;
        }
        try {
            URIBuilder b = new URIBuilder(proxyUri);
            bypasses.forEach(hostPattern -> b.addParameter(ProxyUris.PARAM_BYPASS, hostPattern));
            return b.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
