package io.github.mike10004.debexam;

import static java.util.Objects.requireNonNull;

public interface DebData extends VirtualFilesystemProvider {
    static DebData predefined(VirtualFilesystem filesystem) {
        requireNonNull(filesystem);
        return () -> filesystem;
    }


}
