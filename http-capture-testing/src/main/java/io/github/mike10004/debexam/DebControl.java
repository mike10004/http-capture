package io.github.mike10004.debexam;

import static java.util.Objects.requireNonNull;

public interface DebControl extends VirtualFilesystemProvider {

    static DebControl predefined(VirtualFilesystem filesystem) {
        requireNonNull(filesystem);
        return () -> filesystem;
    }

}
