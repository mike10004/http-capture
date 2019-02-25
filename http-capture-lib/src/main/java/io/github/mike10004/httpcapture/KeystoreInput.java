package io.github.mike10004.httpcapture;

import com.google.common.io.ByteSource;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public interface KeystoreInput {

    ByteSource getBytes();
    String getPassword();
    String getPrivateKeyAlias();

    static KeystoreInput wrap(byte[] bytes, String password, String privateKeyAlias) {
        return wrap(ByteSource.wrap(bytes), password, privateKeyAlias);
    }

    static KeystoreInput wrap(ByteSource bytes, String password, String privateKeyAlias) {
        requireNonNull(bytes);
        return new KeystoreInput() {
            @Override
            public ByteSource getBytes() {
                return bytes;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public String getPrivateKeyAlias() {
                return privateKeyAlias;
            }
        };
    }

    default KeystoreInput copyFrozen() throws IOException {
        byte[] bytes = getBytes().read();
        String pw = getPassword();
        String privateKeyAlias = getPrivateKeyAlias();
        return new KeystoreInput() {
            @Override
            public ByteSource getBytes() {
                return ByteSource.wrap(bytes);
            }

            @Override
            public String getPassword() {
                return pw;
            }

            @Override
            public String getPrivateKeyAlias() {
                return privateKeyAlias;
            }
        };
    }
}
