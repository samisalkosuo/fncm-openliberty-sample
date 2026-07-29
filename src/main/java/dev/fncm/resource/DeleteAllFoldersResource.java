package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.DeleteAllFoldersOperation;
import dev.fncm.service.javaapi.service.buildinginspectiondocs.AddBuildingInspectionDocsOperation;
import dev.fncm.service.javaapi.service.buildinginspectiondocs.DeleteBuildingInspectionDocsOperation;
import dev.fncm.service.javaapi.service.buildinginspectiondocs.FileBuildingInspectionDocsOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/deleteallfolders")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class DeleteAllFoldersResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @DELETE
    public Response deleteAllFolders() {
        return execute(() -> fileNetService.run(new DeleteAllFoldersOperation(), tokenContext));
    }

}

// Made with Bob
