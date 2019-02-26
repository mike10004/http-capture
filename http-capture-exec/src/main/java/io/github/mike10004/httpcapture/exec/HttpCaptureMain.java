package io.github.mike10004.httpcapture.exec;

import com.google.common.annotations.VisibleForTesting;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

public class HttpCaptureMain {

    private static final String OPT_PORT = "port";
    private static final String OPT_EXPORT = "export";
    private static final String OPT_OUTPUT_DIR = "output-dir";
    private static final String OPT_BROWSER = "browser";
    private static final String OPT_KEEP_BROWSER_OPEN = "keep-browser-open";
    private static final String OPT_BROWSER_ARGS = "browser-args";

    @VisibleForTesting
    HttpCaptureMain() {
    }

    public int main0(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new TerseHelpFormatter());
        parser.acceptsAll(Arrays.asList("h", "help"), "print help and exit").forHelp();
        parser.acceptsAll(Arrays.asList("p", OPT_PORT), "listen on specified port")
                .withRequiredArg().ofType(Integer.class).describedAs("PORT");
        parser.acceptsAll(Arrays.asList("x", OPT_EXPORT), "export data from captured or existing HAR file")
                .withOptionalArg().ofType(String.class).describedAs("FILE");
        parser.acceptsAll(Arrays.asList("d", OPT_OUTPUT_DIR), "set output directory")
                .withRequiredArg().ofType(String.class).describedAs("DIR");
        parser.accepts(OPT_BROWSER, "open web browser configured to use capture server")
                .withRequiredArg().ofType(Browser.class).describedAs("BROWSER");
        parser.accepts(OPT_KEEP_BROWSER_OPEN, "do not kill browser when server is stopped");
        parser.accepts(OPT_BROWSER_ARGS, "extra arguments for browser command line; use CSV syntax for multiple args")
                .withRequiredArg().ofType(String.class).describedAs("ARGS");
        OptionSet options = parser.parse(args);
        if (options.has("help")) {
            parser.printHelpOn(stdout());
            return 0;
        }
        HttpCaptureConfig config = parameterizeConfig(parser, options);
        HttpCaptureProgram program = new HttpCaptureProgram(config);
        int exitCode = program.execute();
        return exitCode;
    }

    protected PrintStream stdout() {
        return System.out;
    }

    protected HttpCaptureConfig parameterizeConfig(OptionParser parser, OptionSet options) {
        HttpCaptureConfig config = new HttpCaptureConfig();
        config.port = (Integer) options.valueOf(OPT_PORT);
        if (options.has(OPT_OUTPUT_DIR)) {
            config.outputParent = new File((String)options.valueOf(OPT_OUTPUT_DIR)).toPath();
        }
        config.explode = options.has(OPT_EXPORT);
        config.explodeInput = (String) options.valueOf(OPT_EXPORT);
        config.browser = (Browser) options.valueOf(OPT_BROWSER);
        config.browserArgs = (String) options.valueOf(OPT_BROWSER_ARGS);
        config.keepBrowserOpen = options.has(OPT_KEEP_BROWSER_OPEN);
        return config;
    }

    public static void main(String[] args) throws Exception {
        HttpCaptureMain program = new HttpCaptureMain();
        int exitCode = program.main0(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
