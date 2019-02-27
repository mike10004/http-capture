package io.github.mike10004.httpcapture.dist;

import io.github.mike10004.debexam.DpkgExaminer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class DebIT {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void debFileHasExecutable()  throws Exception {
        File debFile = DebTests.resolveDebFile();
        Path parent = temporaryFolder.newFolder().toPath();
        DpkgExaminer examiner = new DpkgExaminer(parent, DpkgExaminer.ExtractionMode.PERSISTENT);
        examiner.examine(debFile);
        File captureExecutable = parent.resolve("data").resolve("usr").resolve("bin").resolve("httpcapture").toFile();
        assertTrue("capture executable exists: " + captureExecutable, captureExecutable.isFile());
        assertTrue("capture executable has execute permissions: " + captureExecutable, captureExecutable.canExecute());
    }
}
