package io.github.mike10004.httpcapture;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.net.HostAndPort;
import io.github.mike10004.httpcapture.testing.TemporaryDirectory;
import io.github.mike10004.httpcapture.testing.TestClients;
import net.lightbody.bmp.core.har.Har;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class BasicCaptureServerTest {

    private Function<CaptureServerControl, CloseableHttpClient> standardClientFactory() {
        return ctrl -> TestClients.buildStandardClient(HostAndPort.fromParts("127.0.0.1", ctrl.getPort()));
    }

    @Test
    public void testCapture_http() throws Exception {
        testCapture(new URL("http://www.example.com/"), standardClientFactory());
    }

    @Test
    public void testCapture_https() throws Exception {
        testCapture(new URL("https://www.whitehouse.gov/"), ctrl -> TestClients.buildBlindlyTrustingHttpClient(HostAndPort.fromParts("127.0.0.1", ctrl.getPort())));
    }

    @Test
    public void testCapture_https_auto() throws Exception {
        try (TemporaryDirectory dir = TemporaryDirectory.create()) {
            CaptureServer server = BasicCaptureServer.builder()
                    .collectHttps(new AutoCertificateAndKeySource(dir.getRoot()))
                    .build();
            testCapture(server, new URL("https://www.whitehouse.gov/"),
                    ctrl -> TestClients.buildBlindlyTrustingHttpClient(HostAndPort.fromParts("127.0.0.1", ctrl.getPort())),
                    false);
        }
    }

    @Test
    public void testCapture_https_knownTrustSource() throws Exception {
        try (TemporaryDirectory dir = TemporaryDirectory.create()) {
            String privateKeyAlias = "cowabunga";
            AutoCertificateAndKeySource autoSource = new AutoCertificateAndKeySource(dir.getRoot(), privateKeyAlias);
            KeystoreInput keystoreInput = autoSource.acquireKeystoreInput().copyFrozen();
            File keystoreFile = dir.getRoot().resolve("my.keystore").toFile();
            keystoreInput.getBytes().copyTo(Files.asByteSink(keystoreFile));
            KeyStoreStreamCertificateSource fileSource = new AutoCertificateAndKeySource.MemoryKeyStoreCertificateSource(AutoCertificateAndKeySource.KEYSTORE_TYPE, Files.toByteArray(keystoreFile), privateKeyAlias, keystoreInput.getPassword());
            CaptureServer server = BasicCaptureServer.builder()
                    .collectHttps(fileSource)
                    .build();
            testCapture(server, new URL("https://www.whitehouse.gov/"),
                    ctrl -> TestClients.buildTrustingHttpClient(HostAndPort.fromParts("127.0.0.1", ctrl.getPort()), keystoreFile, keystoreInput.getPassword()),
                    false);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private Har testCapture(URL mainUrl, Function<CaptureServerControl, CloseableHttpClient> clientFactory) throws Exception {
        return testCapture(mainUrl, clientFactory, false);
    }

    private Har testCapture(URL mainUrl, Function<CaptureServerControl, CloseableHttpClient> clientFactory, boolean ignoreHar) throws Exception {
        return testCapture(BasicCaptureServer.builder().build(), mainUrl, clientFactory, ignoreHar);
    }

    private Har testCapture(CaptureServer server, URL mainUrl, Function<CaptureServerControl, CloseableHttpClient> clientFactory, boolean ignoreHar) throws Exception {
        AtomicReference<Har> harReference = new AtomicReference<>();
        AtomicInteger responseCounter = new AtomicInteger(0);
        CaptureMonitor monitor = new CaptureMonitor() {
            @Override
            public void harCaptured(Har har) {
                harReference.set(har);
            }

            @Override
            public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
                System.out.format("responseReceived: %s -> %s%n", httpRequest, httpResponse);
                responseCounter.incrementAndGet();
            }
        };
        String html;
        try (CaptureServerControl ctrl = server.start(monitor)) {
            try (CloseableHttpClient client = clientFactory.apply(ctrl);
                    CloseableHttpResponse response = client.execute(new HttpGet(mainUrl.toURI()))) {
                html = EntityUtils.toString(response.getEntity());
            }
        }
        Har har = harReference.get();
        assertNotNull("har", har);
        assertNotEquals("num responses", 0, responseCounter.get());
        assertNotEquals("num entries in HAR", 0, har.getLog().getEntries().size());
        if (!ignoreHar) {
            har.getLog().getEntries().forEach(entry -> {
                System.out.format("%d %s %s", entry.getResponse().getStatus(), entry.getRequest().getMethod(), entry.getRequest().getUrl());
            });
            String harHtml = har.getLog().getEntries().stream()
                    .filter(Objects::nonNull)
                    .filter(entry -> entry.getRequest() != null)
                    .filter(entry -> entry.getRequest().getUrl().equals(mainUrl.toString()))
                    .map(entry -> Strings.nullToEmpty(entry.getResponse().getContent().getText()))
                    .findFirst().orElseThrow(() -> new AssertionError("no entry with URL equal to " + mainUrl));
            assertEquals("html", html, harHtml);
        }
        return har;
    }

    @Test
    public void testCookieConsumption() throws Exception {
        Har har = testCapture(new URL("http://httpbin.org/cookies/set/optimus/prime"), standardClientFactory(), true);
        CookieCollection collection = HarAnalysis.of(har).findCookies();
        List<DeserializableCookie> cookies = collection.makeUltimateCookieList();
        assertNotEquals("num cookies", 0, cookies.size());
        DeserializableCookie primeCookie = cookies.stream().filter(cookie ->  "optimus".equals(cookie.getName())).findAny().orElse(null);
        assertNotNull("cookie with name=optimus", primeCookie);
        assertEquals("value", "prime", primeCookie.getValue());
    }
}
