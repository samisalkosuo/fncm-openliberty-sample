package dev.fncm.auth;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Server-side store that maps an opaque Zen token to the username
 * of the authenticated user.
 *
 * Populated at login by {@link dev.fncm.resource.AuthResource} and
 * consulted on every subsequent request by {@link BearerTokenFilter}.
 *
 * Stored in application scope (lives for the lifetime of the server process).
 * Entries are removed on logout or when the token expires and the browser
 * logs in again.
 *
 * NOTE: this is an in-memory cache — it does not survive server restarts.
 *       For multi-instance deployments, replace with a distributed cache.
 */
@ApplicationScoped
public class TokenCache {

    private static final Logger LOG = Logger.getLogger(TokenCache.class.getName());

    /** Holds username and IAM token for a given Zen token. */
    private record Entry(String username, String iamToken) {}

    private final ConcurrentHashMap<String, Entry> tokenToEntry = new ConcurrentHashMap<>();

    /**
     * Stores a token → username + iamToken mapping after a successful login.
     *
     * @param zenToken opaque Zen token
     * @param username authenticated username
     * @param iamToken IAM token obtained in step 1 of the auth flow
     */
    public void put(String zenToken, String username, String iamToken) {
        tokenToEntry.put(zenToken, new Entry(username, iamToken));
        LOG.fine("TokenCache: stored token for username=" + username
                + " (cache size=" + tokenToEntry.size() + ")");
    }

    /**
     * Looks up the username for a given token.
     *
     * @param zenToken opaque Zen token from the Authorization header
     * @return username, or {@code null} if the token is not in the cache
     */
    public String getUsername(String zenToken) {
        Entry e = tokenToEntry.get(zenToken);
        return e != null ? e.username() : null;
    }

    /**
     * Looks up the IAM token for a given Zen token.
     *
     * @param zenToken opaque Zen token from the Authorization header
     * @return IAM token, or {@code null} if the token is not in the cache
     */
    public String getIamToken(String zenToken) {
        Entry e = tokenToEntry.get(zenToken);
        return e != null ? e.iamToken() : null;
    }

    /**
     * Removes a token from the cache (called on logout).
     *
     * @param zenToken opaque Zen token to invalidate
     */
    public void remove(String zenToken) {
        Entry e = tokenToEntry.remove(zenToken);
        LOG.fine("TokenCache: removed token for username=" + (e != null ? e.username() : "unknown"));
    }
}
