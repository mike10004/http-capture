package io.github.mike10004.debexam;

import java.io.IOException;

public interface VirtualFilesystemProvider {

    VirtualFilesystem getFilesystem() throws IOException;

}
