package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.ListFoldersOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/listfolders")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ListFoldersResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @GET
    public Response listFolders() {
        return execute(() -> fileNetService.run(new ListFoldersOperation(), tokenContext));
    }
}

// Made with Bob
