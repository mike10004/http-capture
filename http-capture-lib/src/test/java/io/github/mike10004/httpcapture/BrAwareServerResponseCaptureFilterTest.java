package io.github.mike10004.httpcapture;

import com.google.common.io.Resources;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.github.mike10004.nanochamp.repackaged.fi.iki.elonen.NanoHTTPD;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.brotli.dec.BrotliInputStream;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for the brotli-aware filter.
 */
public class BrAwareServerResponseCaptureFilterTest {

    private static class RecordingMonitor extends HarCaptureMonitor {
        public final List<HttpInteraction> interactions;

        @SuppressWarnings("unused")
        RecordingMonitor() {
            this(new ArrayList<>());
        }

        RecordingMonitor(List<HttpInteraction> interactions) {
            this.interactions = interactions;
        }

        @Override
        public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
            interactions.add(new HttpInteraction(httpRequest, httpResponse));
        }

    }

    @Test(timeout = 10000L)
    public void endToEnd() throws Exception {
        byte[] brotliBytes = loadBrotliCompressedSample();
        byte[] decompressedBytes = loadUncompressedSample();
        CaptureServer captureServer = BasicCaptureServer.builder().build();
        RecordingMonitor monitor = new RecordingMonitor();
        NanoHTTPD.Response compressedResponse = NanoResponse.status(200)
                .header(HttpHeaders.CONTENT_ENCODING, "br")
                .content(MediaType.PLAIN_TEXT_UTF_8, brotliBytes)
                .build();
        System.out.format("prepared response with compressed bytes: %s%n", new String(new Hex().encode(brotliBytes), US_ASCII));
        NanoServer server = NanoServer.builder().get(session -> compressedResponse).build();
        byte[] responseBytes;
        String url;
        try (NanoControl ctrl = server.startServer()) {
            url = ctrl.baseUri().toString();
            try (CaptureServerControl captureCtrl = captureServer.start(monitor)) {
                HostAndPort proxyAddress = HostAndPort.fromParts("127.0.0.1", captureCtrl.getPort());
                // TODO do not bypass for localhost addresses?
                responseBytes = TestClients.fetch(proxyAddress, new HttpGet(URI.create(url)), response -> EntityUtils.toByteArray(response.getEntity()));
            }
        }
        assertArrayEquals("expect compressed bytes are delivered", brotliBytes, responseBytes);
        ImmutableHttpResponse response = monitor.interactions.stream()
                .filter(pair -> url.equals(pair.request.url.toString()))
                .map(HttpInteraction::getResponse).findAny().orElse(null);
        assertNotNull(response);
        assertEquals("response status", 200, response.status);
        System.out.format("response bytes:%n%s%n%n", StringUtils.abbreviate(new String(new Hex().encode(responseBytes), US_ASCII), 256));
        byte[] harResponseBytes = response.getContentAsBytes().read();
        String harResponseBytesHex = new String(new Hex().encode(harResponseBytes));
        System.out.format("response bytes captured by monitor filter: %s%n", harResponseBytesHex);
        assertArrayEquals("response byte", decompressedBytes, harResponseBytes);

    }

    public static class WithoutMockServerTest {

        @Test
        public void decompressBrotliContents() throws Exception {
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, String.format("http://localhost:%d/blah", 12345));
            BrAwareServerResponseCaptureFilter filter = new BrAwareServerResponseCaptureFilter(request, true);
            byte[] brotliBytes = loadBrotliCompressedSample();
            byte[] decompressedBytes = filter.decompressContents(brotliBytes, BrotliInputStream::new);
            byte[] expected = loadUncompressedSample();
            assertArrayEquals("bytes", expected, decompressedBytes);
        }
    }


    private static byte[] loadUncompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt"));
    }

    private static byte[] loadBrotliCompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt.br"));
    }
}