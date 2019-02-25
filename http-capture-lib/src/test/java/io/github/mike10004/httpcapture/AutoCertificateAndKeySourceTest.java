package io.github.mike10004.httpcapture;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.google.common.net.HostAndPort;
import io.github.mike10004.httpcapture.AutoCertificateAndKeySource.SerializableForm;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AutoCertificateAndKeySourceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generateAndUseCertificate() throws Exception {
        File scratchDir = temporaryFolder.getRoot();
        testCertificateUsage(scratchDir.toPath());
    }

    @Test
    public void invokeLoadTwice() throws Exception {
        Path scratchDir = temporaryFolder.getRoot().toPath();
        try (CountingAutoCertificateAndKeySource certificateAndKeySource = new CountingAutoCertificateAndKeySource(scratchDir, random)) {
            certificateAndKeySource.load();
            certificateAndKeySource.load();
            assertEquals("num generate invocations", 1, certificateAndKeySource.generateInvocations.get());
        }
    }

    private static Random random = new Random(AutoCertificateAndKeySourceTest.class.getName().hashCode());

    private File createTempPathname(Path scratchDir, String suffix) throws IOException {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String name = BaseEncoding.base16().encode(bytes) + suffix;
        File file = scratchDir.resolve(name).toFile();
        if (file.isFile()) {
            throw new IOException("file already exists: " + file);
        }
        return file;
    }

    private static class CountingAutoCertificateAndKeySource extends AutoCertificateAndKeySource {

        private final AtomicInteger generateInvocations = new AtomicInteger();

        public CountingAutoCertificateAndKeySource(Path scratchDir, Random random) {
            super(scratchDir, random);
        }

        @Override
        protected MemoryKeyStoreCertificateSource generate(String keystorePassword, String privateKeyAlias) throws IOException {
            generateInvocations.incrementAndGet();
            return super.generate(keystorePassword, privateKeyAlias);
        }
    }

    private void testCertificateUsage(Path scratchDir) throws IOException {
        try (CountingAutoCertificateAndKeySource certificateAndKeySource = new CountingAutoCertificateAndKeySource(scratchDir, random)) {
            KeystoreInput keystoreInput = certificateAndKeySource.acquireKeystoreInput();
            SerializableForm serializableForm = certificateAndKeySource.createSerializableForm();
            File keystoreFile = createTempPathname(scratchDir, ".keystore");
            File pkcs12File = createTempPathname(scratchDir, ".p12");
            KeystoreFileCreator keystoreFileCreator = new OpensslKeystoreFileCreator(UnitTests.makeKeytoolConfig(), UnitTests.makeOpensslConfig());
            keystoreFileCreator.createPKCS12File(keystoreInput, pkcs12File);
            File pemFile = File.createTempFile("certificate", ".pem", scratchDir.toFile());
            assumeOpensslNotSkipped();
            try {
                keystoreFileCreator.createPemFile(pkcs12File, keystoreInput.getPassword(), pemFile);
            } catch (KeystoreFileCreator.NonzeroExitFromCertProgramException e) {
                System.out.println(e.result.content().stderr());
                throw e;
            }
            Files.write(Base64.getDecoder().decode(serializableForm.keystoreBase64), keystoreFile);
            BasicCaptureServer server = BasicCaptureServer.builder()
                    .collectHttps(certificateAndKeySource)
                    .build();
            String url = "https://example.com/";
            HarCaptureMonitor monitor = new HarCaptureMonitor();
            try (CaptureServerControl ctrl = server.start(monitor)) {
                HostAndPort proxyAddress = HostAndPort.fromParts("127.0.0.1", ctrl.getPort());
                try (CloseableHttpClient client = TestClients.buildTrustingHttpClient(proxyAddress, keystoreFile, keystoreInput.getPassword());
                        CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    EntityUtils.consume(response.getEntity());
                }
            }
            Har har = monitor.getCapturedHar();
            HarResponse response = har.getLog().getEntries().stream()
                    .filter(entry -> url.equals(entry.getRequest().getUrl()))
                    .map(HarEntry::getResponse)
                    .findFirst()
                    .orElse(null);
            assertNotNull("response in har", response);
            assertEquals("response status", 200, response.getStatus());
        }
    }

    private void assumeOpensslNotSkipped() {
        Assume.assumeFalse("openssl tests are skipped by property " + UnitTests.SYSPROP_OPENSSL_TESTS_SKIP, UnitTests.isSkipOpensslTests());
    }
}