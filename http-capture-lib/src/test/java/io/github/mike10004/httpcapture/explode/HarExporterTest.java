package io.github.mike10004.httpcapture.explode;

import com.google.common.io.Files;
import io.github.mike10004.httpcapture.testing.HarTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class HarExporterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void explode() throws Exception {
        HarExporter exploder = new HarExporter();
        HarTestCase testCase = new HarTestCase("/example-captured.har");
        File harFile = testCase.getPathname(temporaryFolder.getRoot().toPath());
        Path outputRoot = temporaryFolder.getRoot().toPath();
        exploder.export(Files.asCharSource(harFile, StandardCharsets.UTF_8), outputRoot);
        testCase.checkExport(outputRoot);
    }
}