package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.DownloadContentElementOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Streams a content element from a FileNet document to the browser as a file download.
 *
 * <p>GET /api/downloaddocument?documentId=&lt;id&gt;&amp;retrievalName=&lt;name&gt;
 *
 * <p>The response carries a {@code Content-Disposition: attachment} header so the browser
 * triggers a native download with the original filename preserved.
 */
@Path("/downloaddocument")
@RequestScoped
@Produces(MediaType.WILDCARD)
public class DownloadDocumentResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @GET
    public Response downloadDocument(
            @QueryParam("documentId")    String documentId,
            @QueryParam("retrievalName") String retrievalName) {

        if (documentId == null || documentId.isBlank()) {
            return error(400, "documentId query parameter is required");
        }
        if (retrievalName == null || retrievalName.isBlank()) {
            return error(400, "retrievalName query parameter is required");
        }

        try {
            return fileNetService.run(
                    new DownloadContentElementOperation(documentId, retrievalName),
                    tokenContext);
        } catch (IllegalArgumentException e) {
            return error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return error(503, e.getMessage());
        } catch (Exception e) {
            return error(500, e.getMessage());
        }
    }
}

// Made with Bob
