package io.github.mike10004.debexam;

import com.github.mike10004.nativehelper.subprocess.ProcessException;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ScopedProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.io.CharSource;
import io.github.mike10004.httpcapture.testing.TemporaryDirectory;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class DpkgExaminer implements DebExaminer {

    private final Path extractionParent;
    private final ExtractionMode extractionMode;

    public DpkgExaminer() {
        this(FileUtils.getTempDirectory().toPath(), ExtractionMode.TEMPORARY);
    }

    public DpkgExaminer(Path extractionParent, ExtractionMode mode) {
        this.extractionMode = requireNonNull(mode);
        this.extractionParent = requireNonNull(extractionParent);
    }

    @Override
    public DebExamination examine(File debFile) throws IOException {
        if (extractionMode == ExtractionMode.PERSISTENT) {
            Path controlRoot = extractionParent.resolve("control");
            Path dataRoot = extractionParent.resolve("data");
            return DpkgExamination.extractPersistently(debFile, controlRoot, dataRoot);
        } else {
            return DpkgExamination.extractTemporarily(debFile, extractionParent);
        }

    }

    public enum ExtractionMode {
        TEMPORARY,
        PERSISTENT
    }

    static String parseDpkgInfoVersion(String infoText) {
        String line1;
        try {
            line1 = CharSource.wrap(infoText).readLines().get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Matcher m = Pattern.compile("^.*\\s+version\\s+(\\S+)\\.$").matcher(line1);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalArgumentException("info text does not match expected pattern");
    }

    private static class DpkgExamination implements DebExamination {

        private final java.io.Closeable finishAction;
        private final DebModel model;

        public DpkgExamination(java.io.Closeable finishAction, DpkgExtraction extraction) {
            this.finishAction = requireNonNull(finishAction);
            this.model = requireNonNull(extraction);
        }

        @Override
        public DebModel getModel() {
            return model;
        }

        @Override
        public void close() throws IOException {
            this.finishAction.close();
        }

        @Nullable
        private static String runClean(String executable, String[] args, boolean collectStdout) throws IOException {
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
            } catch (InterruptedException e) {
                throw new IOException("error waiting for subprocess to terminate", e);
            }
            if (result_.exitCode() != 0) {
                throw new IOException("dpkg --extract failed with code " + result_.exitCode());
            }
            return content;
        }

        public static DpkgExamination extractTemporarily(File debFile, Path tempParent_) throws IOException {
            TemporaryDirectory tempDir = TemporaryDirectory.create(tempParent_);
            Path workingArea = tempDir.getRoot();
            DpkgExtraction extraction;
            try {
                Path controlRoot = workingArea.resolve("control");
                Path dataRoot = workingArea.resolve("data");
                extraction = doExtract(debFile, controlRoot, dataRoot);
            } catch (Exception e) {
                try {
                    tempDir.close();
                } catch (Exception ignore) {
                }
                if (e instanceof IOException) {
                    throw (IOException)e;
                }
                if (!(e instanceof ProcessException)) {
                    throw (RuntimeException)e;
                }
                throw new IOException(e);
            }
            return new DpkgExamination(tempDir, extraction);
        }

        public static DpkgExamination extractPersistently(File debFile, Path controlRoot, Path dataRoot) throws IOException {
            DpkgExtraction extraction = doExtract(debFile, controlRoot, dataRoot);
            DpkgExamination examination = new DpkgExamination(() -> {}, extraction);
            return examination;
        }

        private static DpkgExtraction doExtract(File debFile, Path controlRoot, Path dataRoot) throws IOException {
            runClean("dpkg", new String[]{"--control", debFile.getAbsolutePath(), controlRoot.toString()}, false);
            runClean("dpkg", new String[]{"--extract", debFile.getAbsolutePath(), dataRoot.toString()}, false);
            String infoText = runClean("dpkg", new String[]{"--info", debFile.getAbsolutePath()}, true);
            String versionStr = parseDpkgInfoVersion(infoText) + "\n";
            return DpkgExtraction.onDisk(versionStr, controlRoot, dataRoot);
        }
    }


}
