package dev.fncm.resource;

import dev.fncm.auth.TokenContext;
import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.ListDocumentClassesOperation;
import dev.fncm.service.javaapi.service.ListFoldersOperation;
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

@Path("/listdocumentclasses")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ListDocumentClassesResource {

    private static final Logger LOGGER = Logger.getLogger(ListDocumentClassesResource.class.getName());

    @Inject
    TokenContext tokenContext;

    @Inject
    FileNetService fileNetService;

    @GET
    public Response listDocumentClasses() {
        LOGGER.info("listDocumentClasses enter");
        try {
            String result = fileNetService.run(new ListDocumentClassesOperation(), tokenContext);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}

// Made with Bob
