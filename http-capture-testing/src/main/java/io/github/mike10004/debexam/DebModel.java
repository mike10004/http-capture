package io.github.mike10004.debexam;

import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface DebModel {

    default byte[] getVersion() {
        return BaseEncoding.base16().decode(getVersionHex());
    }

    default String getVersion(Charset charset) {
        return new String(getVersion(), charset);
    }

    default String getVersionAscii() {
        return getVersion(StandardCharsets.US_ASCII);
    }

    String getVersionHex();

    DebControl getControl() throws IOException;

    DebData getData() throws IOException;

}

