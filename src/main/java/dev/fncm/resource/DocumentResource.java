package dev.fncm.resource;

import dev.fncm.auth.TokenCache;
import dev.fncm.auth.TokenContext;
import dev.fncm.service.javaapi.service.ConnectionTest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

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
public class DocumentResource {

    private static final Logger LOGGER = Logger.getLogger(DocumentResource.class.getName());

    /** Populated by BearerTokenFilter with the validated token and username. */
    @Inject
    TokenContext tokenContext;

    @Inject
    TokenCache tokenCache;

    /**
     * GET /api/documents
     * Returns a stub document list; replace with real FileNet / external API calls.
     * Use {@code tokenContext.getZenToken()} to forward the Zen token to external services.
     */
    @GET
    public Response listDocuments(){
        LOGGER.info("listDocuments enter");
        String body = new JSONObject()
                .put("subject", tokenContext.getUsername())
                .put("documents", new JSONArray())
                .toString();
                /*
        try {
        String zenToken = tokenContext.getZenToken();
        String iamToken = tokenContext.getIAMToken();
        LOGGER.info("userName: "+tokenCache.getUsername(zenToken));
        LOGGER.info("iamToken: "+iamToken);
        
        ConnectionTest test = new ConnectionTest(tokenCache.getUsername(zenToken), zenToken);
        LOGGER.info("ConnectionTest: "+test.toString());
        
        test.run(new String[0]);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.INFO,e,null);
            
        }
        */
        return Response.ok(body).build();
    }
}
