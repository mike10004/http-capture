package io.github.mike10004.debexam;

import com.google.common.io.ByteSource;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class DiskFilesystem implements VirtualFilesystem {

    private final Path root;

    public DiskFilesystem(Path root) {
        this.root = requireNonNull(root);
    }

    @Override
    public Stream<String> listDirectories()  throws IOException {
        return java.nio.file.Files.walk(root)
                .filter(p -> p.toFile().isDirectory())
                .map(root::relativize)
                .map(Path::toString);
    }

    @Override
    public boolean isFile(String relativePath) {
        return root.resolve(relativePath).toFile().isFile();
    }

    @Nullable
    @Override
    public ByteSource getFile(String relativePath) {
        return com.google.common.io.Files.asByteSource(root.resolve(relativePath).toFile());
    }

    @Override
    public Stream<String> listFiles() throws IOException {
        return java.nio.file.Files.walk(root)
                .filter(p -> p.toFile().isFile())
                .map(root::relativize)
                .map(Path::toString);
    }

}
