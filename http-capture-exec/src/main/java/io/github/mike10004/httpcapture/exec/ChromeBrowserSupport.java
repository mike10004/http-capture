package io.github.mike10004.httpcapture.exec;

import com.github.mike10004.nativehelper.subprocess.ProcessMonitor;
import com.github.mike10004.nativehelper.subprocess.ProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of browser support for Chrome or Chromium.
 */
public class ChromeBrowserSupport implements BrowserSupport {

    private final String executableName;
    private final OutputDestination outputDestination;

    public ChromeBrowserSupport(String executableName, OutputDestination outputDestination) {
        this.executableName = requireNonNull(executableName).trim();
        if (this.executableName.isEmpty()) {
            throw new IllegalArgumentException("executable name must be nonempty");
        }
        this.outputDestination = outputDestination;
    }

    public enum OutputDestination {
        FILES, CONSOLE
    }

    @Override
    public LaunchableBrowser prepare(Path scratchDir) throws IOException {
        Path userDataDir = java.nio.file.Files.createTempDirectory(scratchDir, "chrome-user-data");
        @Nullable Path outputDir = null;
        if (outputDestination == OutputDestination.FILES) {
            outputDir = java.nio.file.Files.createTempDirectory(scratchDir, "chrome-output");
        }
        return new LaunchableChromish(userDataDir, outputDir);
    }

    static final List<String> DEFAULT_CHROME_ARGS = Collections.unmodifiableList(Arrays.asList(
            "--no-first-run",
            "--ignore-certificate-errors"
    ));

    private class LaunchableChromish implements LaunchableBrowser {

        private final Path userDataDir;
        @Nullable
        private final Path outputDir;

        private final String proxyType;

        private LaunchableChromish(Path userDataDir, @Nullable Path outputDir) {
            this.userDataDir = requireNonNull(userDataDir);
            this.outputDir = outputDir;
            proxyType = "http";
        }

        @Override
        public ProcessMonitor<?, ?> launch(HostAndPort captureServerAddress, Iterable<String> moreArguments, ProcessTracker processTracker) {
            Subprocess.Builder sb = runningChromeOrChromium();
            List<String> defaultChromeArgs = new ArrayList<>(DEFAULT_CHROME_ARGS);
            List<String> moreArgumentsList = ImmutableList.copyOf(moreArguments);
            defaultChromeArgs.removeAll(moreArgumentsList);
            sb.args(defaultChromeArgs);
            sb.arg("--proxy-server=" + proxyType + "://" + captureServerAddress.toString());
            sb.arg("--user-data-dir=" + userDataDir.toFile().getAbsolutePath());
            sb.args(moreArguments);
            sb.arg("data:,"); // start URL
            Subprocess subprocess = sb.build();
            Subprocess.Launcher<?, ?> launcher = subprocess.launcher(processTracker);
            if (outputDir != null) {
                launcher.outputTempFiles(outputDir);
            } else {
                launcher.inheritOutputStreams();
            }
            ProcessMonitor<?, ?> monitor = launcher.launch();
            return monitor;
        }

        protected Subprocess.Builder runningChromeOrChromium() {
            return Subprocess.running(executableName);
        }
    }

    @Override
    public String toString() {
        return "ChromeBrowserSupport{" +
                "outputDestination=" + outputDestination +
                '}';
    }
}
