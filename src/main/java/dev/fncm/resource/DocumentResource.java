package dev.fncm.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Example secured REST resource.
 * Authentication is enforced by {@link dev.fncm.auth.BearerTokenFilter}
 * before this method is ever called.
 */
@Path("/documents")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource extends BaseResource {

    /**
     * GET /api/documents
     * Returns a stub document list; replace with real FileNet / external API calls.
     * Use {@code tokenContext.getZenToken()} to forward the Zen token to external services.
     */
    @GET
    public Response listDocuments() {
        return execute(() -> new JSONObject()
                .put("subject", tokenContext.getUsername())
                .put("documents", new JSONArray())
                .toString());
    }
}
