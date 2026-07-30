package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.buildinginspectiondocs.CreateBuildingInspectionDocumentOperation;
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

import java.time.LocalDate;
import java.util.Date;

/**
 * POST /api/createdocument
 *
 * <p>Accepts a multipart/form-data request with the following parts:
 * <ul>
 *   <li>{@code municipality}      — municipality name</li>
 *   <li>{@code propertyAddress}   — address of the inspected property</li>
 *   <li>{@code inspectorName}     — name of the inspector</li>
 *   <li>{@code inspectionDate}    — date string in YYYY-MM-DD format</li>
 *   <li>{@code buildingType}      — building type choice list value</li>
 *   <li>{@code complianceStatus}  — compliance status choice list value</li>
 *   <li>{@code file}              — binary file to store as the document's content element</li>
 * </ul>
 */
@Path("/createbuildinginspectionreportdocument")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class CreateBuildingInspectionReportDocumentResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createDocument(
            @FormParam("municipality")     String     municipality,
            @FormParam("propertyAddress")  String     propertyAddress,
            @FormParam("inspectorName")    String     inspectorName,
            @FormParam("inspectionDate")   String     inspectionDate,
            @FormParam("buildingType")     String     buildingType,
            @FormParam("complianceStatus") String     complianceStatus,
            @FormParam("file")             EntityPart filePart) {

        return execute(() -> {
            // Parse date (YYYY-MM-DD from <input type="date">)
            Date parsedDate = null;
            if (inspectionDate != null && !inspectionDate.isBlank()) {
                parsedDate = java.sql.Date.valueOf(LocalDate.parse(inspectionDate));
            }

            // Read file bytes and filename from the EntityPart
            byte[] fileBytes = null;
            String fileName  = "unknown";
            if (filePart != null) {
                fileBytes = filePart.getContent().readAllBytes();
                fileName  = filePart.getFileName().orElse("unknown");
            }

            return fileNetService.run(
                    new CreateBuildingInspectionDocumentOperation(
                            municipality,
                            propertyAddress,
                            inspectorName,
                            parsedDate,
                            buildingType,
                            complianceStatus,
                            fileBytes,
                            fileName),
                    tokenContext);
        });
    }
}

// Made with Bob
