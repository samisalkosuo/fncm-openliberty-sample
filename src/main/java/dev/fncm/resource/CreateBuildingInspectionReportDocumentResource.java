package dev.fncm.resource;

import dev.fncm.service.GraphQLService;
import dev.fncm.service.graphql.CreateDocumentMutation;
import dev.fncm.service.javaapi.FileNetConfig;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * POST /api/createbuildinginspectionreportdocument
 *
 * <p>Accepts a multipart/form-data request and creates a document in CP4BA via the
 * {@code createDocument} GraphQL mutation.
 *
 * <p>Accepted form fields:
 * <ul>
 *   <li>{@code municipality}      — municipality name (stored as document property)</li>
 *   <li>{@code propertyAddress}   — address of the inspected property</li>
 *   <li>{@code inspectorName}     — name of the inspector</li>
 *   <li>{@code inspectionDate}    — date string in YYYY-MM-DD format</li>
 *   <li>{@code buildingType}      — building type choice list value</li>
 *   <li>{@code complianceStatus}  — compliance status choice list value</li>
 *   <li>{@code file}              — (optional) binary file to attach as content element</li>
 * </ul>
 *
 * <p>When {@code file} is omitted the document is created without a content element.
 */
@Path("/createbuildinginspectionreportdocument")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class CreateBuildingInspectionReportDocumentResource extends BaseResource {

    @Inject
    FileNetConfig config;

    @Inject
    GraphQLService graphQLService;

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
            // Collect domain-specific properties into the generic properties map
            Map<String, Object> properties = new java.util.LinkedHashMap<>();
            if (municipality     != null) properties.put("Municipality",     municipality);
            if (propertyAddress  != null) properties.put("PropertyAddress",  propertyAddress);
            if (inspectorName    != null) properties.put("InspectorName",    inspectorName);
            if (inspectionDate   != null) properties.put("InspectionDate",   toFileNetDateTime(inspectionDate));
            if (buildingType     != null) properties.put("BuildingType",     buildingType);
            if (complianceStatus != null) properties.put("ComplianceStatus", complianceStatus);

            // Extract file parts — null filePart means no content element
            byte[] fileBytes       = null;
            String fileName        = null;
            String fileContentType = null;
            if (filePart != null) {
                fileBytes       = filePart.getContent().readAllBytes();
                fileName        = filePart.getFileName().orElse("unknown");
                fileContentType = filePart.getMediaType().toString();
            }
            String documentName = String.format("Inspection Report (%s %s)",municipality,inspectionDate);
            CreateDocumentMutation mutation = new CreateDocumentMutation(
                    config.getObjectStore(),
                    null,
                    "BuildingInspectionReport",
                    documentName,
                    properties,
                    fileBytes,
                    fileContentType,
                    fileName);

            if (mutation.hasFile()) {
                return graphQLService.executeMultipart(mutation, tokenContext.getZenToken());
            } else {
                return graphQLService.execute(mutation, tokenContext.getZenToken());
            }
        });
    }
    /**
     * Converts a {@code YYYY-MM-DD} date string to the ISO-8601 datetime format that
     * FileNet expects: {@code 2026-07-02T00:00:00Z} (midnight UTC).
     *
     * @param date a date string in {@code YYYY-MM-DD} format
     * @return formatted datetime string, or the original value if parsing fails
     */
    private static String toFileNetDateTime(String date) {
        try {
            return LocalDate.parse(date).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        } catch (Exception e) {
            return date;
        }
    }
}
