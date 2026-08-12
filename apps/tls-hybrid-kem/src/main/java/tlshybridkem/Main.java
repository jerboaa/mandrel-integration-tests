package tlshybridkem;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Tests that the PQC hybrid KEM named group X25519MLKEM768 is
 * registered and reachable in native image.
 * Tests that a TLS 1.3 handshake using that group succeeds end-to-end.
 *
 * @author Michal Karm Babacek <karm@ibm.com>
 */
public class Main {
    private static final String PING = "PING";
    private static final String PONG = "PONG";

    public static void main(String[] args) throws Exception {
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        final SSLParameters params = ctx.getSupportedSSLParameters();
        final List<String> groups = Arrays.asList(params.getNamedGroups());
        System.out.println("Supported named groups: " + groups);
        if (!groups.contains("X25519MLKEM768")) {
            System.err.println("FAILED: X25519MLKEM768 not found in named groups.");
            System.exit(1);
        }
        System.out.println("OK: X25519MLKEM768 is supported. Proceeding to handshake test...");
        final String ksPath = System.getProperty("javax.net.ssl.keyStore", "server.p12");
        final String ksPass = System.getProperty("javax.net.ssl.keyStorePassword", "password");
        final KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream resStream = Main.class.getResourceAsStream("/" + ksPath);
                InputStream is = resStream != null ? resStream : new java.io.FileInputStream(ksPath)) {
            ks.load(is, ksPass.toCharArray());
        }
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, ksPass.toCharArray());
        final SSLContext serverCtx = SSLContext.getInstance("TLSv1.3");
        serverCtx.init(kmf.getKeyManagers(), null, null);
        final SSLContext clientCtx = SSLContext.getInstance("TLSv1.3");
        // Don't do this at home (or at work). It's for handshake test only.
        clientCtx.init(null, new javax.net.ssl.TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {
                    }
                }
        }, null);
        final CountDownLatch serverReady = new CountDownLatch(1);
        final int[] portHolder = { 0 };
        final String[] negotiatedProtocol = { null };
        final String[] negotiatedCipher = { null };
        final Throwable[] serverError = { null };
        final Thread serverThread = Thread.ofPlatform().start(() -> {
            try (SSLServerSocket serverSocket =
                    (SSLServerSocket) serverCtx.getServerSocketFactory()
                            .createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                serverSocket.setNeedClientAuth(false);
                portHolder[0] = serverSocket.getLocalPort();
                serverReady.countDown();
                try (SSLSocket conn = (SSLSocket) serverSocket.accept()) {
                    conn.startHandshake();
                    final byte[] buf = conn.getInputStream().readNBytes(PING.length());
                    conn.getOutputStream().write(PONG.getBytes());
                    conn.getOutputStream().flush();
                    negotiatedProtocol[0] = conn.getSession().getProtocol();
                    negotiatedCipher[0] = conn.getSession().getCipherSuite();
                    System.out.println(new String(buf, StandardCharsets.US_ASCII));
                }
            } catch (Exception e) {
                serverError[0] = e;
                serverReady.countDown();
            }
        });
        serverReady.await();
        if (serverError[0] != null) {
            throw new RuntimeException("Server failed to start", serverError[0]);
        }
        try (SSLSocket clientSocket =
                (SSLSocket) clientCtx.getSocketFactory()
                        .createSocket(InetAddress.getLoopbackAddress(), portHolder[0])) {
            final SSLParameters clientParams = clientSocket.getSSLParameters();
            clientParams.setProtocols(new String[] { "TLSv1.3" });
            clientParams.setNamedGroups(new String[] { "X25519MLKEM768", "x25519", "secp256r1" });
            clientSocket.setSSLParameters(clientParams);
            clientSocket.startHandshake();
            final OutputStream out = clientSocket.getOutputStream();
            out.write(PING.getBytes());
            out.flush();
            final byte[] response = clientSocket.getInputStream().readNBytes(PONG.length());
            if (!PONG.equals(new String(response))) {
                throw new AssertionError("Unexpected response: " + new String(response));
            }
        }
        serverThread.join(5000);
        if (serverError[0] != null) {
            throw new RuntimeException("Server error during handshake", serverError[0]);
        }
        System.out.println("Handshake successful.");
        System.out.println("Negotiated Protocol: " + negotiatedProtocol[0]);
        System.out.println("Negotiated CipherSuite: " + negotiatedCipher[0]);
        System.out.println("Test passed.");
    }
}
