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
        DpkgExaminer examiner = new DpkgExaminer(temporaryFolder.getRoot().toPath());
        String controlText;
        String binHelloSha256sum;
        Set<String> dataFilesActual;
        Set<String> dataFilesExpected = new HashSet<>(Arrays.asList("usr/bin/hello",
                "usr/share/info/hello.info.gz",
                "usr/share/man/man1/hello.1.gz",
                "usr/share/doc/hello/copyright",
                "usr/share/doc/hello/changelog.Debian.gz",
                "usr/share/doc/hello/NEWS.gz"));
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
    }
}