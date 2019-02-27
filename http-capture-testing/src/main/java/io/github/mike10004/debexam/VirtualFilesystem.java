package io.github.mike10004.debexam;

import com.google.common.io.ByteSource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public interface VirtualFilesystem {

    ByteSource getFile(String relativePath);

    Stream<String> listFiles() throws IOException;

    Stream<String> listDirectories() throws IOException;

    default boolean isFile(String relativePath) {
        requireNonNull(relativePath, "path must be non-null");
        try {
            return listFiles().anyMatch(relativePath::equals);
        } catch (IOException e) {
            return false;
        }
    }

    default VirtualFilesystem freezeIndex() throws IOException {
        return freezeIndex(p -> true);
    }

    default VirtualFilesystem freezeIndex(Predicate<String> pathFilter) throws IOException {
        List<String> files = Collections.unmodifiableList(listFiles().filter(pathFilter).collect(Collectors.toList()));
        List<String> dirs = Collections.unmodifiableList(listDirectories().filter(pathFilter).collect(Collectors.toList()));
        VirtualFilesystem original = this;
        return new VirtualFilesystem() {
            @Nullable
            @Override
            public ByteSource getFile(String relativePath) {
                if (isFile(relativePath)) {
                    @Nullable ByteSource originalFile = original.getFile(relativePath);
                    if (originalFile == null) {
                        return NotFoundByteSource.getInstance();
                    }
                }
                return original.getFile(relativePath);
            }

            @Override
            public boolean isFile(String relativePath) {
                return files.contains(relativePath);
            }

            @Override
            public Stream<String> listFiles() {
                return files.stream();
            }

            @Override
            public Stream<String> listDirectories() {
                return dirs.stream();
            }
        };
    }
}
