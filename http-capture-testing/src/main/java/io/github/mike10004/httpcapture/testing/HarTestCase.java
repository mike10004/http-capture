package io.github.mike10004.httpcapture.testing;

import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertNotEquals;

public class HarTestCase {

    private final String resourcePath;

    public HarTestCase(String resourcePath) {
        this.resourcePath = resourcePath;
        requireNonNull(resourcePath, "resource path must be non-null");
    }

    public File getPathname(Path scratchDir) throws IOException {
        URL resource = getClass().getResource(resourcePath);
        if (resource == null) {
            throw new FileNotFoundException("classpath:" + resourcePath);
        }
        if ("file".equals(resource.getProtocol())) {
            try {
                return new File(resource.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            File tmpFile = File.createTempFile("test-case", ".har", scratchDir.toFile());
            Resources.asByteSource(resource).copyTo(Files.asByteSink(tmpFile));
            return tmpFile;
        }
    }

    public void checkExport(Path exportDir) throws IOException {
        List<File> files = java.nio.file.Files.walk(exportDir).map(Path::toFile)
                .filter(File::isFile)
                .collect(Collectors.toList());
        assertNotEquals("num files exported", 0, files.size());
    }
}
