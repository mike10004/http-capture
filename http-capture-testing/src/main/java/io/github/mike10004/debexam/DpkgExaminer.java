package io.github.mike10004.debexam;

import com.github.mike10004.nativehelper.subprocess.ProcessException;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ScopedProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import io.github.mike10004.httpcapture.testing.TemporaryDirectory;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class DpkgExaminer implements DebExaminer {

    private final Path parentForTempDir;

    public DpkgExaminer() {
        this(FileUtils.getTempDirectory().toPath());
    }

    public DpkgExaminer(Path parentForTempDir) {
        this.parentForTempDir = requireNonNull(parentForTempDir);
    }

    @Override
    public DebExamination examine(File debFile) throws IOException {
        return DpkgExamination.extract(debFile, parentForTempDir);
    }

    private static class DpkgExamination implements DebExamination {

        private final TemporaryDirectory extractionRoot;
        private final DebModel model;

        public DpkgExamination(TemporaryDirectory extractionRoot, DpkgExtraction extraction) {
            this.extractionRoot = requireNonNull(extractionRoot);
            this.model = requireNonNull(extraction);
        }

        @Override
        public DebModel getModel() {
            return model;
        }

        @Override
        public void close() throws IOException {
            this.extractionRoot.close();
        }

        @Nullable
        private static String runClean(String executable, String[] args, boolean collectStdout) throws IOException, InterruptedException {
            Subprocess subprocess = Subprocess.running(executable)
                    .args(Arrays.asList(args))
                    .build();
            ProcessResult<?, ?> result_;
            @Nullable String content;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                if (collectStdout) {
                    ProcessResult<String, String> result = subprocess.launcher(processTracker)
                            .outputStrings(Charset.defaultCharset())
                            .launch().await();
                    content = result.content().stdout();
                    result_ = result;
                } else {
                    result_ = subprocess.launcher(processTracker)
                            .inheritOutputStreams()
                            .launch().await();
                    content = null;
                }
            }
            if (result_.exitCode() != 0) {
                throw new IOException("dpkg --extract failed with code " + result_.exitCode());
            }
            return content;
        }

        public static DpkgExamination extract(File debFile, Path tempParent_) throws IOException {
            TemporaryDirectory tempDir = TemporaryDirectory.create(tempParent_);
            Path workingArea = tempDir.getRoot();
            DpkgExtraction extraction;
            try {
                Path controlRoot = workingArea.resolve("control");
                runClean("dpkg", new String[]{"--control", debFile.getAbsolutePath(), controlRoot.toString()}, false);
                Path dataRoot = workingArea.resolve("data");
                runClean("dpkg", new String[]{"--extract", debFile.getAbsolutePath(), dataRoot.toString()}, false);
                String versionStr = "2.0\n"; // TODO get this from dpkg --info outpu
                extraction = DpkgExtraction.onDisk(versionStr, controlRoot, dataRoot);
            } catch (Exception e) {
                try {
                    tempDir.close();
                } catch (Exception ignore) {
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                }
                if (e instanceof RuntimeException && !(e instanceof ProcessException)) {
                    throw (RuntimeException)e;
                }
                throw new IOException(e);
            }
            return new DpkgExamination(tempDir, extraction);
        }
    }


}
