package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.buildinginspectiondocs.SearchBuildingInspectionOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * GET /api/searchbuildinginspection?q=&lt;text&gt;
 *
 * <p>Searches all {@code BuildingInspectionReport} documents whose custom
 * metadata contains the supplied text.
 */
@Path("/searchbuildinginspection")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SearchBuildingInspectionResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @GET
    public Response search(@QueryParam("q") String q) {
        if (q == null || q.isBlank()) {
            return error(400, "Query parameter 'q' is required");
        }
        return execute(() -> fileNetService.run(
                new SearchBuildingInspectionOperation(q.trim()), tokenContext));
    }
}

// Made with Bob
