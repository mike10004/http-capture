package io.github.mike10004.httpcapture.exec;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.PrintStream;
import java.util.Arrays;

public class HttpCaptureMain {

    private final PrintStream stdout;
    private final PrintStream stderr;

    public HttpCaptureMain() {
        this.stdout = System.out;
        this.stderr = System.err;
    }

    public int main0(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(Arrays.asList("h", "help"), "print help and exit").forHelp();
        OptionSet options = parser.parse(args);
        if (options.has("help")) {
            parser.printHelpOn(stdout);
            return 0;
        }
        return 0;
    }

    public static void main(String[] args) throws Exception {
        HttpCaptureMain program = new HttpCaptureMain();
        int exitCode = program.main0(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

}
