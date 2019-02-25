package io.github.mike10004.httpcapture;

import com.google.common.net.HostAndPort;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

class TestClients {

    private TestClients() {}

    public static CloseableHttpClient buildStandardClient(HostAndPort proxy) {
        HttpClientBuilder b = HttpClients.custom()
                .useSystemProperties();
        if (proxy != null) {
            b.setProxy(new HttpHost(proxy.getHost(), proxy.getPort()));
        }
        b.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        return b.build();

    }


    public static void configureClientToTrustBlindly(HttpClientBuilder clientBuilder) throws GeneralSecurityException {
        configureClientToTrust(clientBuilder, SSLContexts.custom()
                .loadTrustMaterial(blindTrustStrategy())
                .build());
    }
    public static void configureClientToTrust(HttpClientBuilder clientBuilder, SSLContext sslContext) throws GeneralSecurityException {
        clientBuilder.setSSLContext(sslContext);
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, blindHostnameVerifier());
        clientBuilder.setSSLSocketFactory(sslsf);
        clientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
    }

    public static javax.net.ssl.HostnameVerifier blindHostnameVerifier() {
        return new BlindHostnameVerifier();
    }

    public static TrustStrategy blindTrustStrategy() {
        return new BlindTrustStrategy();
    }

    private static final class BlindHostnameVerifier implements javax.net.ssl.HostnameVerifier {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    private static final class BlindTrustStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            return true;
        }

    }

    public static CloseableHttpClient buildBlindlyTrustingHttpClient(HostAndPort proxy) {
        HttpClientBuilder b = HttpClients.custom()
                .useSystemProperties();
        if (proxy != null) {
            b.setProxy(new HttpHost(proxy.getHost(), proxy.getPort()));
        }
        b.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        try {
            configureClientToTrustBlindly(b);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        return b.build();
    }

    public static CloseableHttpClient buildTrustingHttpClient(HostAndPort proxy, File keystoreFile, String keystorePassword) {
        HttpClientBuilder b = HttpClients.custom()
                .useSystemProperties();
        if (proxy != null) {
            b.setProxy(new HttpHost(proxy.getHost(), proxy.getPort()));
        }
        b.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(keystoreFile, keystorePassword.toCharArray(), null)
                    .build();
            configureClientToTrust(b, sslContext);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
        return b.build();
    }

    public static String fetchTextWithBlindTrust(HostAndPort proxyAddress, HttpUriRequest request) throws IOException {
        return fetchWithBlindTrust(proxyAddress, request, new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse response) throws IOException {
                return EntityUtils.toString(response.getEntity());
            }
        });
    }

    public static <R> R fetchWithBlindTrust(HostAndPort proxyAddress, HttpUriRequest request, ResponseHandler<R> responseHandler) throws IOException {
        try (CloseableHttpClient client = buildBlindlyTrustingHttpClient(proxyAddress)) {
            return client.execute(request, responseHandler);
        }
    }

    public static String fetchText(HostAndPort proxyAddress, HttpUriRequest request) throws IOException {
        return fetch(proxyAddress, request, new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse response) throws IOException {
                return EntityUtils.toString(response.getEntity());
            }
        });
    }

    public static <R> R fetch(HostAndPort proxyAddress, HttpUriRequest request, ResponseHandler<R> responseHandler) throws IOException {
        try (CloseableHttpClient client = buildStandardClient(proxyAddress)) {
            return client.execute(request, responseHandler);
        }
    }
}
