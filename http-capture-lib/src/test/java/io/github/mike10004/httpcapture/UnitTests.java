package io.github.mike10004.httpcapture;

import com.google.common.base.Strings;

import java.io.File;

public class UnitTests {

    public static final String SYSPROP_OPENSSL_TESTS_SKIP = "selenium-help.openssl.tests.skip";
    private static final String SYSPROP_OPENSSL_EXECUTABLE = "selenium-help.openssl.executable.path";

    public static ExecutableConfig makeOpensslConfig() {
        String path = Strings.emptyToNull(System.getProperty(SYSPROP_OPENSSL_EXECUTABLE));
        if (path != null) {
            File file = new File(path);
            System.out.format("using openssl executable at %s%n", file);
            return ExecutableConfig.byPathOnly(file);
        }
        return ExecutableConfig.byNameOnly("openssl");
    }

    public static ExecutableConfig makeKeytoolConfig() {
        return ExecutableConfig.byNameOnly("keytool");
    }

    public static boolean isSkipOpensslTests() {
        return Boolean.parseBoolean(System.getProperty(SYSPROP_OPENSSL_TESTS_SKIP, "false"));
    }

}
