package io.github.mike10004.httpcapture.exec;

import net.lightbody.bmp.core.har.Har;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public interface OutputSink {

    Writer openStream(Har har) throws IOException;

    @Nullable
    default File mostRecentFile() {
        return null;
    }

    default String describe() {
        return toString();
    }

    static OutputSink toFileInParent(Path parentDir, Charset charset) {
        return new FileOutputSink(parentDir, charset);
    }

    class FileOutputSink implements OutputSink {

        private final Path parentDir;
        private final Charset charset;
        private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        private File mostRecentFile;

        public FileOutputSink(Path parentDir, Charset charset) {
            this.parentDir = parentDir;
            this.charset = charset;
        }

        @Nullable
        @Override
        public File mostRecentFile() {
            return mostRecentFile;
        }

        @Override
        public Writer openStream(Har har) throws IOException {
            File outputFile = parentDir.resolve(constructFilename(har)).toFile();
            mostRecentFile = outputFile;
            com.google.common.io.Files.createParentDirs(outputFile);
            return new OutputStreamWriter(new FileOutputStream(outputFile), charset);
        }

        protected String constructFilename(Har har) {
            return String.format("http-capture-%s.har", timestamp());
        }

        protected String timestamp() {
            return dateFormat.format(Date.from(Instant.now()));
        }

        @Override
        public String describe() {
            File mostRecentFile = this.mostRecentFile;
            if (mostRecentFile == null) {
                return "file in " + parentDir;
            }
            return mostRecentFile.getAbsolutePath();
        }
    }
}
