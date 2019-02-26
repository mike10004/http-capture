package io.github.mike10004.httpcapture.explode;

import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class HarExporterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void explode() throws Exception {
        HarExporter exploder = new HarExporter();
        File harFile = new File(getClass().getResource("/example-captured.har").toURI());
        Path outputRoot = temporaryFolder.getRoot().toPath();
        exploder.export(Files.asCharSource(harFile, StandardCharsets.UTF_8), outputRoot);
        List<File> files = java.nio.file.Files.walk(outputRoot).map(Path::toFile)
                .filter(File::isFile)
                .collect(Collectors.toList());
        assertNotEquals("num files exported", 0, files.size());
    }
}