package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.__NAME__Operation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/__PATH__")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class __NAME__Resource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @GET
    public Response __PATH__() {
        return execute(() -> fileNetService.run(new __NAME__Operation(), tokenContext));
    }
}

// Made with Bob
