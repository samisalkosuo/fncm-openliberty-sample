package dev.fncm.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/**
 * JAX-RS filter that runs on every /api/* request.
 *
 * Reads the {@code Authorization: Bearer <token>} header, looks up the token
 * in the server-side {@link TokenCache} (populated at login), and populates
 * {@link TokenContext} for injection into resources.
 *
 * Requests without a valid token receive a plain 401 JSON response —
 * no WWW-Authenticate header, so the browser never shows a login dialog.
 *
 * The login endpoint {@code /api/auth/login} is explicitly excluded so the
 * browser can authenticate without an existing token.
 */
@Provider
public class BearerTokenFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(BearerTokenFilter.class.getName());

    private static final String AUTH_HEADER   = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    TokenCache tokenCache;

    @Inject
    TokenContext tokenContext;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String method = ctx.getMethod();
        String raw    = ctx.getUriInfo().getRequestUri().getPath();
        LOG.info("BearerTokenFilter: " + method + " " + raw);

        // Pass public endpoints through without requiring a token.
        if (raw.contains("/auth/") || raw.endsWith("/config")) {
            LOG.info("BearerTokenFilter: skipping auth check for public path");
            return;
        }

        String authHeader = ctx.getHeaderString(AUTH_HEADER);
        LOG.info("BearerTokenFilter: Authorization header present = " + (authHeader != null)
                + (authHeader != null ? ", starts with Bearer = " + authHeader.startsWith(BEARER_PREFIX) : ""));

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOG.warning("BearerTokenFilter: REJECTED – missing or invalid Authorization header"
                    + " [path=" + raw + "]");
            abort(ctx, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            LOG.warning("BearerTokenFilter: REJECTED – empty Bearer token [path=" + raw + "]");
            abort(ctx, "Empty Bearer token");
            return;
        }

        LOG.info("BearerTokenFilter: token length=" + token.length()
                + ", first 10 chars=" + token.substring(0, Math.min(10, token.length())));

        String username = tokenCache.getUsername(token);
        if (username == null) {
            LOG.warning("BearerTokenFilter: REJECTED – token not found in cache [path=" + raw + "]"
                    + " (session expired or server was restarted)");
            abort(ctx, "Token not recognised – please log in again");
            return;
        }

        String iamToken = tokenCache.getIamToken(token);
        LOG.info("BearerTokenFilter: token valid, username=" + username);
        tokenContext.set(token, iamToken, username);
    }

    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response
                .status(Response.Status.UNAUTHORIZED)
                .header("Content-Type", "application/json")
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}
