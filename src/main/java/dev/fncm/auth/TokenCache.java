package dev.fncm.auth;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Server-side store that maps an opaque Zen token to the username
 * of the authenticated user.
 *
 * Populated at login by {@link dev.fncm.resource.AuthResource} and
 * consulted on every subsequent request by {@link BearerTokenFilter}.
 *
 * Entries expire after {@code token.ttl.seconds} (default 3600).
 * Expiry is enforced lazily on every read and eagerly by a background
 * sweep that runs at half the TTL interval.
 *
 * NOTE: this is an in-memory cache — it does not survive server restarts.
 *       For multi-instance deployments, replace with a distributed cache.
 */
@ApplicationScoped
public class TokenCache {

    private static final Logger LOG = Logger.getLogger(TokenCache.class.getName());

    /** Holds username, IAM token, and the instant this entry expires. */
    private record Entry(String username, String iamToken, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Inject
    @ConfigProperty(name = "token.ttl.seconds", defaultValue = "3600")
    long ttlSeconds;

    private final ConcurrentHashMap<String, Entry> tokenToEntry = new ConcurrentHashMap<>();
    private ScheduledExecutorService sweeper;

    @PostConstruct
    void startSweeper() {
        sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "token-cache-sweeper");
            t.setDaemon(true);
            return t;
        });
        long sweepInterval = Math.max(ttlSeconds / 2, 60);
        sweeper.scheduleAtFixedRate(this::evictExpired, sweepInterval, sweepInterval, TimeUnit.SECONDS);
        LOG.fine("TokenCache: sweeper started (interval=" + sweepInterval + "s, ttl=" + ttlSeconds + "s)");
    }

    @PreDestroy
    void stopSweeper() {
        if (sweeper != null) {
            sweeper.shutdownNow();
        }
    }

    /**
     * Stores a token → username + iamToken mapping after a successful login.
     *
     * @param zenToken   opaque Zen token
     * @param username   authenticated username
     * @param iamToken   IAM token obtained in step 1 of the auth flow
     * @param ttlSeconds time-to-live in seconds for this entry
     */
    public void put(String zenToken, String username, String iamToken, long ttlSeconds) {
        tokenToEntry.put(zenToken,
                new Entry(username, iamToken, Instant.now().plusSeconds(ttlSeconds)));
        LOG.fine("TokenCache: stored token for username=" + username
                + " (cache size=" + tokenToEntry.size() + ", ttl=" + ttlSeconds + "s)");
    }

    /**
     * Looks up the username for a given token; returns {@code null} if the
     * token is absent or has expired (expired entry is removed eagerly).
     */
    public String getUsername(String zenToken) {
        Entry e = getValid(zenToken);
        return e != null ? e.username() : null;
    }

    /**
     * Looks up the IAM token for a given Zen token; returns {@code null} if
     * the token is absent or has expired.
     */
    public String getIamToken(String zenToken) {
        Entry e = getValid(zenToken);
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

    // --- internals ---

    /** Returns the entry only if it exists and has not expired; removes it otherwise. */
    private Entry getValid(String zenToken) {
        Entry e = tokenToEntry.get(zenToken);
        if (e == null) {
            return null;
        }
        if (e.isExpired()) {
            tokenToEntry.remove(zenToken, e);
            LOG.fine("TokenCache: lazily evicted expired token for username=" + e.username());
            return null;
        }
        return e;
    }

    /** Background sweep — removes all expired entries at once. */
    private void evictExpired() {
        int[] removed = {0};
        tokenToEntry.forEach((token, entry) -> {
            if (entry.isExpired()) {
                tokenToEntry.remove(token, entry);
                removed[0]++;
            }
        });
        if (removed[0] > 0) {
            LOG.fine("TokenCache: sweep evicted " + removed[0] + " expired entries"
                    + " (remaining=" + tokenToEntry.size() + ")");
        }
    }
}
