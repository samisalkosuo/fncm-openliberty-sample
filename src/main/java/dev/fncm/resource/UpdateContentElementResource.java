package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.UpdateContentElementOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * POST /api/contentElement
 *
 * <p>Accepts a multipart/form-data request and adds or replaces content elements
 * on a checked-out reservation document (without checking it in).
 *
 * <p>Accepted form fields:
 * <ul>
 *   <li>{@code reservationId} — ID of the checked-out reservation document</li>
 *   <li>{@code mode}          — {@code "add"} to append, {@code "replace"} to replace all</li>
 *   <li>{@code file}          — binary file to attach as a content element</li>
 * </ul>
 */
@Path("/contentElement")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class UpdateContentElementResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateContentElement(
            @FormParam("documentId")    String     documentId,
            @FormParam("reservationId") String     reservationId,
            @FormParam("mode")          String     mode,
            @FormParam("file")          EntityPart filePart) {

        return execute(() -> {
            byte[] fileBytes       = filePart.getContent().readAllBytes();
            String fileName        = filePart.getFileName().orElse("unknown");
            String fileContentType = filePart.getMediaType().toString();

            return fileNetService.run(
                    new UpdateContentElementOperation(documentId, reservationId, mode, fileBytes, fileName, fileContentType),
                    tokenContext);
        });
    }
}

// Made with Bob
