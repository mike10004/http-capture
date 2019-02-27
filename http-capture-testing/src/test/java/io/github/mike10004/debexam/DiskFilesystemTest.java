package io.github.mike10004.debexam;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class DiskFilesystemTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void listFiles() throws Exception {
        Path top = temporaryFolder.newFolder().toPath();
        String[] relFilePaths = {
                "foo/bar/baz.txt",
                "a.txt",
                "gaw/.hidden"
        };
        Arrays.stream(relFilePaths).forEach(rp -> {
            File f = top.resolve(rp).toFile();
            try {
                com.google.common.io.Files.createParentDirs(f);
                com.google.common.io.Files.touch(f);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });
        VirtualFilesystem fs = new DiskFilesystem(top).freezeIndex();
        Set<String> actualFiles = fs.listFiles().collect(Collectors.toSet());
        assertEquals("files", new HashSet<>(Arrays.asList(relFilePaths)), actualFiles);
    }
}