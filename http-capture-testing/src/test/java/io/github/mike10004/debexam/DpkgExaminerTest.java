package io.github.mike10004.debexam;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import org.apache.commons.lang3.StringUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DpkgExaminerTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void examine() throws Exception {
        File debFile = new File(getClass().getResource("/hello_2.10-1build1_amd64.deb").toURI());
        DpkgExaminer examiner = new DpkgExaminer(temporaryFolder.getRoot().toPath(), DpkgExaminer.ExtractionMode.TEMPORARY);
        String controlText;
        String binHelloSha256sum;
        Set<String> dataFilesActual;
        Set<String> dataFilesExpected = new HashSet<>(Arrays.asList("usr/bin/hello",
                "usr/share/info/hello.info.gz",
                "usr/share/man/man1/hello.1.gz",
                "usr/share/doc/hello/copyright",
                "usr/share/doc/hello/changelog.Debian.gz",
                "usr/share/doc/hello/NEWS.gz"));
        String actualVersionAscii;
        try (DebExamination examination = examiner.examine(debFile)) {
            DebModel model = examination.getModel();
            ByteSource controlBytes = model.getControl().getFilesystem().getFile("control");
            assertNotNull("'control' file present in control root", controlBytes);
            controlText = controlBytes.asCharSource(StandardCharsets.UTF_8).read();
            VirtualFilesystem dataFilesystem = model.getData().getFilesystem();
            dataFilesActual = dataFilesystem.listFiles().collect(Collectors.toSet());
            ByteSource binHelloBytes = dataFilesystem.getFile("usr/bin/hello");
            assertNotNull("usr/bin/hello", binHelloBytes);
            binHelloSha256sum = binHelloBytes.hash(Hashing.sha256()).toString().toLowerCase();
            actualVersionAscii = model.getVersionAscii();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new AssertionError(e);
        }
        System.out.println(controlText);
        String packageName = CharSource.wrap(controlText).readLines().stream().filter(line -> line.startsWith("Package: "))
                .map(line -> StringUtils.removeStart(line, "Package: "))
                .findFirst().orElseThrow(() -> new AssertionError("Package: line not found"));
        assertEquals("package name", "hello", packageName);
        assertEquals("/usr/bin/hello sha256sum", "8b3156dc995e82ec1b26d9ee44e6856e61d308b701cfc0c5ec2154e286fb7c0b", binHelloSha256sum);
        assertEquals("data files", dataFilesExpected, dataFilesActual);
        assertEquals("version ascii", "2.0\n", actualVersionAscii);
    }

    @Test
    public void parseVersion() throws Exception {
        String infoText = " new Debian package, version 2.0.\n" +
                " size 27248 bytes: control archive=879 bytes.\n" +
                "     841 bytes,    21 lines      control              \n" +
                "     375 bytes,     6 lines      md5sums              \n" +
                " Package: hello\n" +
                " Version: 2.10-1build1\n" +
                " Architecture: amd64\n" +
                " Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>\n" +
                " Original-Maintainer: Santiago Vila <sanvila@debian.org>\n" +
                " Installed-Size: 108\n" +
                " Depends: libc6 (>= 2.14)\n" +
                " Conflicts: hello-traditional\n" +
                " Breaks: hello-debhelper (<< 2.9)\n" +
                " Replaces: hello-debhelper (<< 2.9), hello-traditional\n" +
                " Section: devel\n" +
                " Priority: optional\n" +
                " Homepage: http://www.gnu.org/software/hello/\n" +
                " Description: example package based on GNU hello\n" +
                "  The GNU hello program produces a familiar, friendly greeting.  It\n" +
                "  allows non-programmers to use a classic computer science tool which\n" +
                "  would otherwise be unavailable to them.\n" +
                "  .\n" +
                "  Seriously, though: this is an example of how to do a Debian package.\n" +
                "  It is the Debian version of the GNU Project's `hello world' program\n" +
                "  (which is itself an example for the GNU Project).\n";
        String versionTrimmed = DpkgExaminer.parseDpkgInfoVersion(infoText);
        assertEquals("version", "2.0", versionTrimmed);
    }
}