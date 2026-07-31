package dev.fncm.model;

/**
 * Returned to the browser after a successful login.
 * The accessToken is the raw OAuth token from the upstream IdP;
 * the appToken is the application-issued JWT used for /api/* calls.
 *
 * The {@code config} field carries non-sensitive server configuration values
 * (repositoryIdentifier, domain, stanza) so JavaScript card developers can
 * reference them via {@code session.config} without an extra round-trip.
 */
public class LoginResponse {

    private String appToken;
    private String accessToken;  // upstream OAuth token (for direct GraphQL calls)
    private long expiresIn;
    private AppConfig config;

    public LoginResponse() {}

    public LoginResponse(String appToken, String accessToken, long expiresIn, AppConfig config) {
        this.appToken = appToken;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.config = config;
    }

    public String    getAppToken()    { return appToken; }
    public String    getAccessToken() { return accessToken; }
    public long      getExpiresIn()   { return expiresIn; }
    public AppConfig getConfig()      { return config; }

    public void setAppToken(String appToken)       { this.appToken = appToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setExpiresIn(long expiresIn)       { this.expiresIn = expiresIn; }
    public void setConfig(AppConfig config)        { this.config = config; }
}
