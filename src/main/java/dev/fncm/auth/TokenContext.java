package dev.fncm.auth;

import jakarta.enterprise.context.RequestScoped;

/**
 * Carries the validated CP4BA Zen token, an optional IAM token, and the username
 * for the current request.
 * Populated by {@link BearerTokenFilter} and injected into secured resources.
 */
@RequestScoped
public class TokenContext {

    private String zenToken;
    private String iamToken;
    private String username;

    /** Returns the CP4BA Zen (bearer) token for the current request. */
    public String getZenToken() { return zenToken; }

    /**
     * Returns the IAM token, or {@code null} when it was not stored
     * (i.e. requests authenticated via {@link BearerTokenFilter}).
     */
    public String getIAMToken() { return iamToken; }

    public String getUsername() { return username; }

    /**
     * Populates the context with a Zen token and username.
     * Used by {@link BearerTokenFilter} for every authenticated /api/* request.
     */
    public void set(String zenToken, String username) {
        this.zenToken = zenToken;
        this.username = username;
    }

    /**
     * Populates the context with both tokens and username.
     * Used after a successful two-step login when both tokens are available.
     */
    public void set(String zenToken, String iamToken, String username) {
        this.zenToken = zenToken;
        this.iamToken = iamToken;
        this.username = username;
    }

    public boolean isAuthenticated() {
        return zenToken != null && !zenToken.isBlank();
    }
}
