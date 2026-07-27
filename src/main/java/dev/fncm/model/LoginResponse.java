package dev.fncm.model;

/**
 * Returned to the browser after a successful login.
 * The accessToken is the raw OAuth token from the upstream IdP;
 * the appToken is the application-issued JWT used for /api/* calls.
 */
public class LoginResponse {

    private String appToken;
    private String accessToken;  // upstream OAuth token (for direct GraphQL calls)
    private long expiresIn;

    public LoginResponse() {}

    public LoginResponse(String appToken, String accessToken, long expiresIn) {
        this.appToken = appToken;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAppToken()    { return appToken; }
    public String getAccessToken() { return accessToken; }
    public long   getExpiresIn()   { return expiresIn; }

    public void setAppToken(String appToken)       { this.appToken = appToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setExpiresIn(long expiresIn)        { this.expiresIn = expiresIn; }
}
