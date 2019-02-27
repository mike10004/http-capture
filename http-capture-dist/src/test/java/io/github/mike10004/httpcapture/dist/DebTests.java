package io.github.mike10004.httpcapture.dist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DebTests {

    private DebTests() {
    }

    public static File resolveDebFile() throws IOException {
        String snapshotDebPathname = getWellDefinedProperty("deb.pathname.snapshot");
        String releaseDebPathname = getWellDefinedProperty("deb.pathname.release");
        String projectVersion = getWellDefinedProperty("maven.project.version");
        String debPathname;
        if (projectVersion.endsWith("-SNAPSHOT")) {
            debPathname = snapshotDebPathname;
        } else {
            debPathname = releaseDebPathname;
        }
        File debFile = new File(debPathname);
        if (!debFile.isFile()) {
            throw new FileNotFoundException(debFile.getAbsolutePath());
        }
        return debFile;
    }

    private static String getWellDefinedProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isEmpty() || (value.startsWith("${") && value.endsWith("}"))) {
            throw new IllegalArgumentException("property " + name + " is not well-defined; value is " + value);
        }
        return value;
    }
}
