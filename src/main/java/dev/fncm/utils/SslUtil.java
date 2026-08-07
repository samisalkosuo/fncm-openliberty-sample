package dev.fncm.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class for intentional TLS bypass in development/non-production environments.
 *
 * <p><strong>Security warning:</strong> these methods disable all certificate and hostname
 * validation. They must never be used in production against untrusted networks.
 */
public final class SslUtil {

    private SslUtil() {}

    /** A trust-all {@link TrustManager} that accepts every certificate without validation. */
    private static final TrustManager TRUST_ALL = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        public void checkClientTrusted(X509Certificate[] c, String a) {}
        public void checkServerTrusted(X509Certificate[] c, String a) {}
    };

    /**
     * Applies a trust-all SSL context to a single {@link HttpsURLConnection}.
     * Call this before {@code connect()} / {@code getOutputStream()}.
     *
     * @param https  the connection to configure
     * @param logger caller's logger used to emit a warning; may be {@code null}
     * @param urlStr the URL string, used only in the warning message
     * @throws IOException if the SSL context cannot be initialised
     */
    @SuppressWarnings("java:S4830")  // intentional trust-all
    public static void applyTrustAllToConnection(HttpsURLConnection https, Logger logger, String urlStr)
            throws IOException {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{TRUST_ALL}, new SecureRandom());
            https.setSSLSocketFactory(ctx.getSocketFactory());
            https.setHostnameVerifier((host, session) -> true);
            if (logger != null) {
                logger.warning("TLS certificate verification DISABLED for: " + urlStr);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to configure trust-all SSL context", e);
        }
    }

    /**
     * Installs a trust-all SSL socket factory and hostname verifier as the JVM-wide defaults.
     * This affects all subsequent {@link HttpsURLConnection} instances in the same JVM.
     *
     * @param logger caller's logger used to emit info/warning messages; may be {@code null}
     * @throws GeneralSecurityException if the SSL context cannot be initialised
     */
    @SuppressWarnings("java:S4830")  // intentional trust-all
    public static void configureGlobalTrustAll(Logger logger) throws GeneralSecurityException {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[]{TRUST_ALL}, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        if (logger != null) {
            logger.info("SSL configured (trust-all — development only)");
        }
    }
}
