package io.github.mike10004.httpcapture.testing;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public interface TemporaryDirectory extends java.io.Closeable {

    static TemporaryDirectory create() throws IOException {
        return TemporaryDirectory.create(FileUtils.getTempDirectory().toPath());
    }

    static TemporaryDirectory create(Path parent) throws IOException {
        return new TemporaryDirectoryImpl(TemporaryDirectoryImpl.createSubdirectory(parent));
    }

    Path getRoot();

}

class TemporaryDirectoryImpl implements TemporaryDirectory {

    private Path root;

    public TemporaryDirectoryImpl(Path root) {
        this.root = requireNonNull(root);
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public void close() throws IOException {
        try {
            FileUtils.deleteDirectory(root.toFile());
        } catch (IOException e) {
            // suppress exception if path does not exist; maybe it was already deleted, which is cool with us
            if (root.toFile().exists()) {
                throw e;
            }
        }
    }

    public static Path createSubdirectory(Path parent) throws IOException {
        Path child = java.nio.file.Files.createTempDirectory(parent, "TemporaryDirectory");
        return child;
    }
}
