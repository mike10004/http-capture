package io.github.mike10004.httpcapture.exec;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StreamBucket extends PrintStream {

    private ByteArrayOutputStream collector;
    private Charset charset;

    public StreamBucket() throws UnsupportedEncodingException {
        this(new ByteArrayOutputStream(1024), StandardCharsets.UTF_8);
    }

    private StreamBucket(ByteArrayOutputStream baos, Charset charset) throws UnsupportedEncodingException {
        super(baos, true, charset.name());
        this.collector = baos;
        this.charset = charset;
    }

    public String dump() {
        return new String(collector.toByteArray(), charset);
    }
}
