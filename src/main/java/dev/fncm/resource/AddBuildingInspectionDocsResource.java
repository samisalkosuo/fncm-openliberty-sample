package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.AddBuildingInspectionDocsOperation;
import dev.fncm.service.javaapi.service.DeleteBuildingInspectionDocsOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/buildinginspectiondocs")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AddBuildingInspectionDocsResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @POST
    public Response addBuildingInspectionDocs() {
        return execute(() -> fileNetService.run(new AddBuildingInspectionDocsOperation(), tokenContext));
    }

    @DELETE
    public Response deleteBuildingInspectionDocs() {
        return execute(() -> fileNetService.run(new DeleteBuildingInspectionDocsOperation(), tokenContext));
    }    
}

// Made with Bob
