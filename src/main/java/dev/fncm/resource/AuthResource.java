package dev.fncm.resource;

import dev.fncm.model.LoginRequest;
import dev.fncm.model.LoginResponse;
import dev.fncm.service.AuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Public (no-JWT) login endpoint.
 * POST /api/auth/login  { "username": "...", "password": "..." }
 *
 * Calls the two-step CP4BA/IAM token flow and returns:
 *   { appToken, accessToken, expiresIn }
 *
 *  - accessToken – Zen token forwarded to the browser for direct GraphQL calls
 *  - appToken    – same token used as Bearer for /api/* REST calls
 */
@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource extends BaseResource {

    @Inject
    AuthService authService;

    @Inject
    @ConfigProperty(name = "token.ttl.seconds", defaultValue = "3600")
    long tokenTtlSeconds;

    @POST
    @Path("/login")
    public Response login(LoginRequest req) {
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            return error(400, "username and password are required");
        }

        try {
            // Step 1: IAM token
            String iamToken = authService.getOauthToken(req.getUsername(), req.getPassword());
            // Step 2: Zen token
            String zenToken = authService.getZenToken(req.getUsername(), iamToken);

            // Register token → username in the server-side cache so the
            // BearerTokenFilter can resolve the username on subsequent requests.
            tokenCache.put(zenToken, req.getUsername(), iamToken, tokenTtlSeconds);

            LoginResponse body = new LoginResponse(
                    zenToken,              // appToken  – Bearer for /api/* calls
                    zenToken,              // accessToken – raw Zen token for direct GraphQL calls
                    (int) tokenTtlSeconds  // expiresIn (seconds)
            );
            return Response.ok(body).build();

        } catch (IllegalStateException e) {
            return error(503, e.getMessage());
        } catch (Exception e) {
            return error(401, "Authentication failed");
        }
    }
}
