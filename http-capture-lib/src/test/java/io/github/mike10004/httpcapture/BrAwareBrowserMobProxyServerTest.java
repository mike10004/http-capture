package io.github.mike10004.httpcapture;

import com.google.common.net.HostAndPort;
import io.github.mike10004.httpcapture.testing.TestClients;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BrAwareBrowserMobProxyServerTest {

    @Test
    public void decodeDuringHarCapture() throws Exception {
        CaptureServer server = BasicCaptureServer.builder()
                // the collector uses the BrAwareBrowserMobProxyServer by default, so there is no need to specify it here
                .build();
        // TODO set up a local webserver that servers a brotli page instead of hitting this external one
        String brotliUrl = "https://tools-7.kxcdn.com/css/all.min.css";
        String brotliPageSource;
        HarCaptureMonitor monitor = new HarCaptureMonitor();
        try (CaptureServerControl ctrl = server.start(monitor)) {
            HostAndPort proxyAddress = HostAndPort.fromParts("127.0.0.1", ctrl.getPort());
            brotliPageSource = TestClients.fetchTextWithBlindTrust(proxyAddress, new HttpGet(brotliUrl));
        }
        Har har = monitor.getCapturedHar();
        assertNotNull("har", har);
        HarResponse response = har.getLog().getEntries().stream()
                .filter(entry -> brotliUrl.equals(entry.getRequest().getUrl()))
                .map(HarEntry::getResponse)
                .findFirst().orElse(null);
        assertNotNull("expect har contains entry with request to " + brotliUrl, response);
        String actualPageSource = response.getContent().getText();
        assertEquals("page source", brotliPageSource.trim(), actualPageSource.trim());
    }

}