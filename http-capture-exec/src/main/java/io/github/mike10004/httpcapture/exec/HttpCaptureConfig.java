package io.github.mike10004.httpcapture.exec;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

class HttpCaptureConfig {
    public Integer port = null;
    public PrintStream stdout = System.out;
    public PrintStream stderr = System.err;
    public Charset charset = StandardCharsets.UTF_8;
    public Path parentDir = new File(System.getProperty("user.dir")).toPath();
    public Function<Path, File> outputPathnamer = parent -> parent.resolve("http-capture.har").toFile();
    public Path tempdirParent = FileUtils.getTempDirectory().toPath();
    public boolean suppressInteractionEcho = false;
}
