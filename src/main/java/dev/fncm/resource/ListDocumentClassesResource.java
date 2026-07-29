package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.ListDocumentClassesOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/listdocumentclasses")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ListDocumentClassesResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @GET
    public Response listDocumentClasses(
            @QueryParam("includeHidden") @DefaultValue("false") boolean includeHidden) {
        return execute(() -> fileNetService.run(new ListDocumentClassesOperation(includeHidden), tokenContext));
    }
}

// Made with Bob
