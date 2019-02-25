package io.github.mike10004.httpcapture;

import org.junit.Assert;
import org.junit.Test;

public class BrAwareBrowserMobProxyServerTest {

    @Test
    public void decodeDuringHarCapture() throws Exception {
        Assert.fail("not yet implemented");
//        TrafficCollector collector = TrafficCollector.builder(webDriverFactory)
//                // the collector uses the BrAwareBrowserMobProxyServer by default, so there is no need to specify it here
//                .build();
//        // TODO set up a local webserver that servers a brotli page instead of hitting this external one
//        String brotliUrl = "https://tools-7.kxcdn.com/css/all.min.css";
//        HarPlus<String> collection = collector.collect(driver -> {
//            driver.get(brotliUrl);
//            return driver.getPageSource();
//        });
//        String brotliPageSource = collection.result;
//        brotliPageSource = UnitTests.removeHtmlWrapping(brotliPageSource);
//        HarResponse response = collection.har.getLog().getEntries().stream()
//                .filter(entry -> brotliUrl.equals(entry.getRequest().getUrl()))
//                .map(HarEntry::getResponse)
//                .findFirst().orElse(null);
//        assertNotNull("expect har contains entry with request to " + brotliUrl, response);
//        String actualPageSource = response.getContent().getText();
//        assertEquals("page source", brotliPageSource.trim(), actualPageSource.trim());
    }

}