package dev.fncm.resource;

import dev.fncm.auth.TokenCache;
import dev.fncm.auth.TokenContext;
import dev.fncm.service.javaapi.service.ConnectionTest;
import dev.fncm.service.javaapi.service.ListFolders;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Example secured REST resource.
 * Authentication is enforced by {@link dev.fncm.auth.BearerTokenFilter}
 * before this method is ever called.
 */
@Path("/listfolders")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ListFoldersResource {

    private static final Logger LOGGER = Logger.getLogger(ConnectionTestResource.class.getName());

    /** Populated by BearerTokenFilter with the validated token and username. */
    @Inject
    TokenContext tokenContext;

    @Inject
    TokenCache tokenCache;

    @GET
    public Response listFolders() {
        LOGGER.info("listFolders enter");
        try {
            String zenToken = tokenContext.getZenToken();
            //String iamToken = tokenContext.getIAMToken();
            // LOGGER.info("userName: "+tokenCache.getUsername(zenToken));
            // LOGGER.info("iamToken: "+iamToken);

            ListFolders svc = new ListFolders(tokenCache.getUsername(zenToken), zenToken);

            String result = svc.run(new String[0]);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, e, null);
            return Response.status(Status.INTERNAL_SERVER_ERROR.ordinal(), e.toString()).build();

        }
    }
}
