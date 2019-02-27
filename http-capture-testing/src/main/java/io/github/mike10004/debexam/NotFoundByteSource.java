package io.github.mike10004.debexam;

import com.google.common.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;

final class NotFoundByteSource extends ByteSource {

    private static final ByteSource SINGLETON = new NotFoundByteSource();

    private NotFoundByteSource() {}

    public static ByteSource getInstance() {
        return SINGLETON;
    }

    @Override
    public InputStream openStream() throws IOException {
        return throwNotFound();
    }

    @Override
    public boolean isEmpty() throws IOException {
        return throwNotFound();
    }

    @Override
    public long size() throws IOException {
        return throwNotFound();
    }

    private static <T> T throwNotFound() throws IOException {
        throw new IOException("not found");
    }

}
