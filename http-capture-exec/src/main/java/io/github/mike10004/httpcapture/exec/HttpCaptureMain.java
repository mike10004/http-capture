package io.github.mike10004.httpcapture.exec;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.PrintStream;
import java.util.Arrays;

public class HttpCaptureMain {

    private static final String OPT_PORT = "port";

    HttpCaptureMain() {
    }

    public int main0(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(Arrays.asList("h", "help"), "print help and exit").forHelp();
        parser.acceptsAll(Arrays.asList("p", OPT_PORT), "listen on specified port")
                .withRequiredArg().ofType(Integer.class).describedAs("PORT");
        OptionSet options = parser.parse(args);
        if (options.has("help")) {
            parser.printHelpOn(stdout());
            return 0;
        }
        HttpCaptureConfig config = parameterizeConfig(parser, options);
        HttpCaptureProgram program = new HttpCaptureProgram(config.stdout, config.stderr);
        int exitCode = program.execute(config);
        return exitCode;
    }

    protected PrintStream stdout() {
        return System.out;
    }

    protected HttpCaptureConfig parameterizeConfig(OptionParser parser, OptionSet options) {
        HttpCaptureConfig config = new HttpCaptureConfig();
        config.port = (Integer) options.valueOf(OPT_PORT);
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
