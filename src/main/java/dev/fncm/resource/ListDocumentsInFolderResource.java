package dev.fncm.resource;

import dev.fncm.service.GraphQLClient;
import dev.fncm.service.GraphQLService;
import dev.fncm.service.graphql.ListDocumentsInFolderQuery;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/listdocumentsinfolder")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ListDocumentsInFolderResource extends BaseResource {

    @Inject
    GraphQLService graphQLService;

    @Inject
    @ConfigProperty(name = "filenet.objectstore")
    String objectStore;

    /**
     * GET /api/listdocumentsinfolder?folder=/path/to/folder
     *
     * Lists documents and sub-folders in the given folder path.
     * The repository (object store) identifier is taken from the
     * {@code filenet.objectstore} config property.
     *
     * @param folder folder path, e.g. {@code /BuildingInspectionReports/ByDate/2025/04}
     */
    @GET
    public Response listDocumentsInFolder(@QueryParam("folder") String folder) {
        if (folder == null || folder.isBlank()) {
            return error(400, "Query parameter 'folder' is required");
        }
        try {
            String responseBody = graphQLService.execute(
                    new ListDocumentsInFolderQuery(objectStore, folder),
                    tokenContext.getZenToken());
            return Response.ok(responseBody).build();
        } catch (GraphQLClient.GraphQLException e) {
            return error(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return error(502, e.getMessage());
        }
    }
}

// Made with Bob
