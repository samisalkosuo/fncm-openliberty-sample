package dev.fncm.resource;

import dev.fncm.service.GraphQLClient;
import dev.fncm.service.GraphQLService;
import dev.fncm.service.graphql.GetUserGroupsQuery;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/getusergroups")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GetUserGroupsResource extends BaseResource {

    @Inject
    GraphQLService graphQLService;

    @GET
    public Response getUserGroups() {

        try {
            String responseBody = graphQLService.execute(new GetUserGroupsQuery(), tokenContext.getZenToken());
            return Response.ok(responseBody).build();
        } catch (GraphQLClient.GraphQLException e) {
            return error(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return error(502, e.getMessage());
        }
    }
}
