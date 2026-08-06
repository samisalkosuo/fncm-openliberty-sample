package dev.fncm.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.json.JSONObject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Two-step CP4BA token flow (mirrors get-oauth-token.java / get-token.sh):
 *
 *   Step 1 – {@link #getOauthToken}: IAM token
 *     POST {iam.host}/idprovider/v1/auth/identitytoken
 *       grant_type=password&username=…&password=…&scope=openid
 *     → access_token (IAM token)
 *
 *   Step 2 – {@link #getZenToken}: Zen token
 *     GET {cp4ba.host}/v1/preauth/validateAuth
 *       header: username, iam-token
 *     → accessToken (Zen token)
 */
@ApplicationScoped
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    @Inject
    @ConfigProperty(name = "iam.host")
    Optional<String> iamHost;

    @Inject
    @ConfigProperty(name = "cp4ba.host")
    Optional<String> cp4baHost;

    /** When false, TLS certificate verification is disabled (self-signed certs). */
    @Inject
    @ConfigProperty(name = "tls.certificate.verification.enabled", defaultValue = "true")
    boolean tlsVerificationEnabled;

    /**
     * Step 1 – exchanges user credentials for an IAM token from the IAM host.
     *
     * @param username login name from the browser
     * @param password plain-text password from the browser
     * @return IAM access token string
     * @throws IllegalStateException when {@code iam.host} is not configured or the call fails
     */
    public String getOauthToken(String username, String password) throws Exception {
        String iamHostVal = iamHost.orElse("").trim();
        if (iamHostVal.isEmpty()) {
            throw new IllegalStateException("iam.host must be configured before calling getOauthToken()");
        }

        String iamUrl   = iamHostVal + "/idprovider/v1/auth/identitytoken";
        String formBody = "grant_type=password"
                + "&username=" + urlEncode(username)
                + "&password=" + urlEncode(password)
                + "&scope=openid";

        LOGGER.info("Step 1: POST " + iamUrl);
        String iamResponse = httpPost(iamUrl,
                "application/x-www-form-urlencoded;charset=UTF-8", formBody);
        LOGGER.fine("IAM response length: " + iamResponse.length());

        String iamToken = new JSONObject(iamResponse).optString("access_token", null);
        if (iamToken == null || iamToken.isEmpty()) {
            throw new IllegalStateException(
                    "Failed to obtain IAM token from " + iamUrl
                    + ". Response: " + iamResponse);
        }
        LOGGER.info("IAM token obtained, length: " + iamToken.length());
        return iamToken;
    }

    /**
     * Step 2 – exchanges an IAM token for a Zen (CP4BA) access token.
     *
     * @param username login name (must match the one used to obtain the IAM token)
     * @param iamToken IAM token obtained from {@link #getOauthToken}
     * @return Zen access token string
     * @throws IllegalStateException when {@code cp4ba.host} is not configured or the call fails
     */
    public String getZenToken(String username, String iamToken) throws Exception {
        String cp4baHostVal = cp4baHost.orElse("").trim();
        if (cp4baHostVal.isEmpty()) {
            throw new IllegalStateException("cp4ba.host must be configured before calling getZenToken()");
        }

        String zenUrl = cp4baHostVal + "/v1/preauth/validateAuth";
        LOGGER.info("Step 2: GET " + zenUrl);
        String zenResponse = httpGet(zenUrl, username, iamToken);
        LOGGER.fine("Zen response: " + zenResponse);

        String zenToken = new JSONObject(zenResponse).optString("accessToken", null);
        if (zenToken == null || zenToken.isEmpty()) {
            throw new IllegalStateException(
                    "Failed to obtain Zen token from " + zenUrl
                    + ". Response: " + zenResponse);
        }
        LOGGER.info("Zen token length: " + zenToken.length());
        return zenToken;
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────

    /**
     * POST a GraphQL JSON body to {@code urlStr} with:
     *   - {@code Authorization: Bearer <zenToken>}
     *   - A randomly generated {@code ECM-CS-XSRF-Token} in both the request
     *     header and the {@code Cookie} header, as required by Content Services GraphQL.
     *
     * @param urlStr   fully-qualified GraphQL endpoint URL
     * @param zenToken CP4BA Zen access token
     * @param jsonBody GraphQL request JSON ({"query":"…","variables":{…}})
     * @return a String[2] where [0] is the response body and [1] is the HTTP status code
     */
    public String[] httpPostGraphQL(String urlStr, String zenToken, String jsonBody) throws IOException {
        String xsrfToken = java.util.UUID.randomUUID().toString();

        HttpURLConnection conn = openConnection(urlStr);
        try {            
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",       "application/json");
            conn.setRequestProperty("Accept",             "application/json");
            conn.setRequestProperty("Authorization",      "Bearer " + zenToken);
            conn.setRequestProperty("ECM-CS-XSRF-Token", xsrfToken);
            conn.setRequestProperty("Cookie",             "ECM-CS-XSRF-Token=" + xsrfToken);

            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }
            int status = conn.getResponseCode();
            return new String[]{readResponse(conn), String.valueOf(status)};
        } finally {
            conn.disconnect();
        }
    }

    /**
     * POST a GraphQL multipart request to {@code urlStr}.
     *
     * <p>The body contains two parts:
     * <ol>
     *   <li>{@code graphql} — {@code application/json} — the GraphQL JSON envelope</li>
     *   <li>{@code fileFieldName} — {@code fileContentType} — raw file bytes</li>
     * </ol>
     *
     * <p>Same XSRF + Bearer auth as {@link #httpPostGraphQL}.
     *
     * @param urlStr          fully-qualified GraphQL endpoint URL
     * @param zenToken        CP4BA Zen access token
     * @param jsonBody        GraphQL JSON envelope ({@code {"query":"…","variables":{…}}})
     * @param fileFieldName   multipart field name for the file part (e.g. {@code "contvar"})
     * @param fileBytes       raw file content
     * @param fileContentType MIME type of the file (e.g. {@code "application/pdf"})
     * @param fileName        original filename for Content-Disposition
     * @return a String[2] where [0] is the response body and [1] is the HTTP status code
     */
    public String[] httpPostGraphQLMultipart(
            String urlStr,
            String zenToken,
            String jsonBody,
            String fileFieldName,
            byte[] fileBytes,
            String fileContentType,
            String fileName) throws IOException {

        String boundary  = java.util.UUID.randomUUID().toString();
        String xsrfToken = java.util.UUID.randomUUID().toString();

        // -- Part 1: graphql JSON envelope -----------------------------------
        byte[] part1Headers = (
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"graphql\"\r\n" +
                "Content-Type: application/json\r\n" +
                "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] part1Body = jsonBody.getBytes(StandardCharsets.UTF_8);

        // -- Part 2: file bytes ----------------------------------------------
        String safeFileName = (fileName != null) ? fileName : "file";
        byte[] part2Headers = (
                "\r\n--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + safeFileName + "\"\r\n" +
                "Content-Type: " + fileContentType + "\r\n" +
                "\r\n").getBytes(StandardCharsets.UTF_8);

        // -- Closing boundary ------------------------------------------------
        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        int contentLength = part1Headers.length + part1Body.length
                          + part2Headers.length + fileBytes.length
                          + closing.length;

        HttpURLConnection conn = openConnection(urlStr);
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",       "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Accept",             "application/json");
            conn.setRequestProperty("Authorization",      "Bearer " + zenToken);
            conn.setRequestProperty("ECM-CS-XSRF-Token", xsrfToken);
            conn.setRequestProperty("Cookie",             "ECM-CS-XSRF-Token=" + xsrfToken);
            conn.setRequestProperty("Content-Length",     String.valueOf(contentLength));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(part1Headers);
                os.write(part1Body);
                os.write(part2Headers);
                os.write(fileBytes);
                os.write(closing);
            }
            int status = conn.getResponseCode();
            return new String[]{readResponse(conn), String.valueOf(status)};
        } finally {
            conn.disconnect();
        }
    }

    /** POST with form-encoded body, returns the response body as a String. */
    private String httpPost(String urlStr, String contentType, String body) throws IOException {
        HttpURLConnection conn = openConnection(urlStr);
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", contentType);
            conn.setRequestProperty("Accept", "application/json");

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * GET with CP4BA preauth headers:
     *   username  – plain username
     *   iam-token – IAM access token obtained in step 1
     */
    private String httpGet(String urlStr, String username, String iamToken) throws IOException {
        HttpURLConnection conn = openConnection(urlStr);
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("username", username);
            conn.setRequestProperty("iam-token", iamToken);
            conn.setRequestProperty("Accept", "application/json");
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    @SuppressWarnings("java:S4830")  // intentional trust-all when verification is disabled
    private HttpURLConnection openConnection(String urlStr) throws IOException {
        
        URL connectionURL = null;
        try {
            connectionURL = new URI(urlStr).toURL();
        }
        catch (URISyntaxException use)
        {
            throw new IOException(use.getMessage());
        }
        HttpURLConnection conn = (HttpURLConnection) connectionURL.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        if (!tlsVerificationEnabled && conn instanceof HttpsURLConnection https) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }}, new java.security.SecureRandom());
                https.setSSLSocketFactory(ctx.getSocketFactory());
                https.setHostnameVerifier((host, session) -> true);
                LOGGER.warning("TLS certificate verification DISABLED for: " + urlStr);
            } catch (Exception e) {
                throw new IOException("Failed to configure trust-all SSL context", e);
            }
        }
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream stream = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            return "";
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
