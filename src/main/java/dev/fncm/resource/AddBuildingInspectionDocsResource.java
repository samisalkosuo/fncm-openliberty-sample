package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.AddBuildingInspectionDocsOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/addbuildinginspectiondocs")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AddBuildingInspectionDocsResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @GET
    public Response addBuildingInspectionDocs() {
        return execute(() -> fileNetService.run(new AddBuildingInspectionDocsOperation(), tokenContext));
    }
}

// Made with Bob
