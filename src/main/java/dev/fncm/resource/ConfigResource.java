package dev.fncm.resource;

import dev.fncm.service.DevConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Public (no-auth) endpoint that exposes optional dev convenience values.
 *
 * GET /api/config
 *
 * Returns { "devUsername": "...", "devPassword": "..." } when the
 * DEV_USER_NAME / DEV_USER_PASSWORD environment variables are set, or null
 * for each field that is absent. The browser uses these to pre-fill the login
 * form in development environments only.
 */
@Path("/config")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource extends BaseResource {

    @Inject
    DevConfig devConfig;

    @GET
    public Response getConfig() {
        Map<String, String> body = new HashMap<>();
        body.put("devUsername", devConfig.getUsername().orElse(null));
        body.put("devPassword", devConfig.getPassword().orElse(null));
        return Response.ok(body).build();
    }
}
