package dev.fncm.resource;

import dev.fncm.service.GraphQLClient;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Server-side proxy for the CP4BA Content Services GraphQL API.
 *
 * POST /api/graphql
 *
 * The browser sends the same Bearer token it uses for all /api/* calls.
 * BearerTokenFilter validates it and populates TokenContext before this
 * resource is invoked.
 *
 * The request body must be a GraphQL JSON envelope:
 *   {"query":"…"}                           – query only
 *   {"query":"…","variables":{…}}           – query + variables
 *   {"query":"…","operationName":"…",...}   – full envelope
 *
 * GraphQL execution (XSRF token, headers, TLS) is handled by {@link GraphQLClient}.
 */
@Path("/graphql")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GraphQLProxyResource extends BaseResource {

    @Inject
    GraphQLClient graphQLClient;

    /**
     * POST /api/graphql
     *
     * @param body GraphQL JSON envelope from the browser
     * @return upstream GraphQL response, status code preserved
     */
    @POST
    public Response proxy(String body) {
        if (body == null || body.isBlank()) {
            return error(400, "Empty GraphQL request body");
        }

        try {
            String responseBody = graphQLClient.executeJson(body, tokenContext.getZenToken());
            return Response.ok(responseBody).build();
        } catch (GraphQLClient.GraphQLException e) {
            return error(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return error(502, e.getMessage());
        }
    }
}
