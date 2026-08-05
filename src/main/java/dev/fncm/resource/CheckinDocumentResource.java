package dev.fncm.resource;

import dev.fncm.model.CheckinDocumentRequest;
import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.CheckinDocumentOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/checkindocument")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CheckinDocumentResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @POST
    public Response checkinDocument(CheckinDocumentRequest req) {
        return execute(() -> fileNetService.run(
                new CheckinDocumentOperation(
                        req.documentId(),
                        req.reservationId(),
                        req.municipality(),
                        req.propertyAddress(),
                        req.inspectorName(),
                        req.inspectionDate(),
                        req.buildingType(),
                        req.complianceStatus()),
                tokenContext));
    }
}

// Made with Bob
