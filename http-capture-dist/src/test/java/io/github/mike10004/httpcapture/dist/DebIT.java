package io.github.mike10004.httpcapture.dist;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class DebIT {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void debExists()  throws Exception {
        File debFile = DebTests.resolveDebFile();
        assertTrue("deb file exists at " + debFile, debFile.length() > 0);
    }
}
