package io.github.mike10004.httpcapture;

import com.google.common.io.ByteSource;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Representation of the data and parameters required to read a key from a keystore.
 */
public interface KeystoreInput {

    /**
     * Gets a supplier of a byte stream containing the keystore bytes.
     * @return the byte source
     */
    ByteSource getBytes();

    /**
     * Gets the keystore password.
     * @return
     */
    String getPassword();

    /**
     * Gets the alias of the relevant private key in the keystore.
     * @return the private key alias
     */
    String getPrivateKeyAlias();

    /**
     * Creates a keystore input object.
     */
    static KeystoreInput wrap(byte[] bytes, String password, String privateKeyAlias) {
        return wrap(ByteSource.wrap(bytes), password, privateKeyAlias);
    }

    /**
     * Creates a keystore input object.
     * @param bytes the byte source
     * @param password the password
     * @param privateKeyAlias the key alias
     * @return the new instance
     */
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

            @Override
            public String toString() {
                return String.format("KeystoreInput{keystore.bytes.length=%d}", bytes.length);
            }
        };
    }
}
