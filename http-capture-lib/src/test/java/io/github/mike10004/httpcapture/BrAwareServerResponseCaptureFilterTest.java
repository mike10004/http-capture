package io.github.mike10004.httpcapture;

import com.google.common.io.Resources;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.brotli.dec.BrotliInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test for the brotli-aware filter. Uses Chrome because JBrowserDriver does not support brotli decoding.
 */
public class BrAwareServerResponseCaptureFilterTest {

    @Test(timeout = 10000L)
    public void endToEnd_chrome() throws Exception {
        Assert.fail("not yet implemented");
//        byte[] brotliBytes = loadBrotliCompressedSample();
//        byte[] decompressedBytes = loadUncompressedSample();
//        String decompressedText = new String(decompressedBytes, UTF_8);
//
//        /*
//         * For some reason, headless Chrome doesn't render the page text, so we
//         * use xvfb here
//         */
//        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
//                .chromeOptions(UnitTests.createChromeOptions())
//                .environment(xvfb.getController().newEnvironment())
//                .build();
//        TrafficCollector collector = TrafficCollector.builder(webDriverFactory).build();
//        RecordingMonitor monitor = new RecordingMonitor();
//        NanoHTTPD.Response compressedResponse = NanoResponse.status(200)
//                .header(HttpHeaders.CONTENT_ENCODING, "br")
//                .content(MediaType.PLAIN_TEXT_UTF_8, brotliBytes)
//                .build();
//        System.out.format("prepared response with compressed bytes: %s%n", new String(new Hex().encode(brotliBytes)));
//        NanoServer server = NanoServer.builder().get(session -> compressedResponse).build();
//        String pageText;
//        String url;
//        try (NanoControl ctrl = server.startServer()) {
//            url = ctrl.baseUri().toString();
//            TrafficGenerator<String> generator = driver -> {
//                driver.get(url);
//                WebElement body = driver.findElement(By.tagName("body"));
//                return body.getText();
//            };
//            pageText = collector.monitor(generator, monitor);
//        }
//        ImmutableHttpResponse response = monitor.interactions.stream()
//                .filter(pair -> url.equals(pair.request.url.toString()))
//                .map(HttpInteraction::getResponse).findAny().orElse(null);
//        assertNotNull(response);
//        assertEquals("response status", 200, response.status);
//        System.out.format("page text:%n%s%n%n", StringUtils.abbreviate(pageText, 256));
//        byte[] harResponseBytes = response.getContentAsBytes().read();
//        String harResponseBytesHex = new String(new Hex().encode(harResponseBytes));
//        System.out.format("response bytes captured by monitor filter: %s%n", harResponseBytesHex);
//        assertArrayEquals("response byte", decompressedBytes, harResponseBytes);
//        assertEquals("pageText", decompressedText, pageText);
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