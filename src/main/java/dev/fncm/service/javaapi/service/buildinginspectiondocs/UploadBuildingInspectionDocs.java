package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.property.Properties;
import com.filenet.api.util.Id;

import dev.fncm.service.javaapi.service.DateUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

public class UploadBuildingInspectionDocs {

    private static final Logger LOGGER = Logger.getLogger(UploadBuildingInspectionDocs.class.getName());

    private static final String CLASS_SYMBOLIC_NAME = BuildingInspectionConstants.DOC_CLASS;
    private static final String RESOURCES_BASE = "/building_inspection_sample_docs";
    private static final String JSON_RESOURCE_PATH = RESOURCES_BASE + "/extract_fields.json";

    public void execute(ObjectStore objectStore) throws Exception {

        LOGGER.info("=================================================");
        LOGGER.info("Starting Document Upload");
        LOGGER.info("=================================================");
        LOGGER.info("Object Store: " + objectStore.get_DisplayName());
        LOGGER.info("Document Class: " + CLASS_SYMBOLIC_NAME);

        // Read JSON resource
        InputStream jsonStream = getClass().getResourceAsStream(JSON_RESOURCE_PATH);
        if (jsonStream == null) {
            throw new Exception("JSON resource not found: " + JSON_RESOURCE_PATH);
        }

        LOGGER.info("Reading JSON resource: " + JSON_RESOURCE_PATH);
        Gson gson = new Gson();
        JsonObject jsonRoot = gson.fromJson(new InputStreamReader(jsonStream), JsonObject.class);

        JsonArray results = jsonRoot.getAsJsonArray("results");
        int totalDocuments = jsonRoot.get("total_documents").getAsInt();

        LOGGER.info("Total documents to upload: " + totalDocuments);

        int successCount = 0;
        int failureCount = 0;

        // Process each document
        for (int i = 0; i < results.size(); i++) {
            JsonObject docInfo = results.get(i).getAsJsonObject();
            String filePath = docInfo.get("file").getAsString();
            JsonObject fields = docInfo.getAsJsonObject("fields");

            try {
                LOGGER.info("-------------------------------------------");
                LOGGER.info("Processing document " + (i + 1) + " of " + totalDocuments);
                LOGGER.info("File: " + filePath);

                // Upload document
                uploadDocument(objectStore, filePath, fields);
                successCount++;
                LOGGER.info("✓ Document uploaded successfully");

            } catch (Exception e) {
                failureCount++;
                LOGGER.severe("✗ Failed to upload document: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Summary
        LOGGER.info("=================================================");
        LOGGER.info("Upload Summary");
        LOGGER.info("=================================================");
        LOGGER.info("Total documents: " + totalDocuments);
        LOGGER.info("Successfully uploaded: " + successCount);
        LOGGER.info("Failed: " + failureCount);
        LOGGER.info("=================================================");

    }

    /**
     * Upload a single document to FileNet with its properties.
     */
    private void uploadDocument(ObjectStore objectStore, String relativeFilePath, JsonObject fields) throws Exception {
        // Normalize path separator so resource lookup works on all platforms
        String resourcePath = RESOURCES_BASE + "/" + relativeFilePath.replace('\\', '/');

        InputStream docStream = getClass().getResourceAsStream(resourcePath);
        if (docStream == null) {
            throw new Exception("Document resource not found: " + resourcePath);
        }

        LOGGER.info("  Resource path: " + resourcePath);

        // Create document instance
        Document doc = Factory.Document.createInstance(objectStore, CLASS_SYMBOLIC_NAME);

        // Set document title (use filename without extension)
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        //String title = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String title = createDocumentName(fields);
        doc.getProperties().putValue("DocumentTitle", title);

        LOGGER.info("  Document Title: " + title);

        // Set properties from JSON fields
        setDocumentProperties(doc, fields);

        // Add content element
        try {
            com.filenet.api.core.ContentTransfer ct = Factory.ContentTransfer.createInstance();
            ct.setCaptureSource(docStream);
            ct.set_RetrievalName(fileName);
            ct.set_ContentType("text/markdown; charset=UTF-8");
            doc.set_ContentElements(Factory.ContentElement.createList());
            doc.get_ContentElements().add(ct);

            LOGGER.info("  Content element added: " + fileName);

            // Save document
            doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
            doc.save(RefreshMode.REFRESH);

            // Get document ID
            Id docId = doc.get_Id();
            LOGGER.info("  Document ID: " + docId.toString());

        } finally {
            docStream.close();
        }
    }

    private String createDocumentName(JsonObject fields) throws Exception {

        StringBuilder title = new StringBuilder("Inspection Report ");
        title.append("(");
        // Municipality (String)
        if (fields.has(BuildingInspectionConstants.PROP_MUNICIPALITY)) {
            String municipality = fields.get(BuildingInspectionConstants.PROP_MUNICIPALITY).getAsString();
            title.append(municipality);
            title.append(" ");
        }        
        // InspectionDate (DateTime)
        if (fields.has(BuildingInspectionConstants.PROP_INSPECTION_DATE)) {
            String dateStr = fields.get(BuildingInspectionConstants.PROP_INSPECTION_DATE).getAsString();
            Date inspectionDate = DateUtil.parseDdMmYyyy(dateStr);
            if (inspectionDate != null) {
                title.append(new SimpleDateFormat("yyyy-MM-dd").format(inspectionDate));
            }
        }
        title.append(")");
        return title.toString();
    }

    /**
     * Set document properties from JSON fields.
     */
    private void setDocumentProperties(Document doc, JsonObject fields) throws Exception {
        Properties props = doc.getProperties();

        // InspectionDate (DateTime)
        if (fields.has(BuildingInspectionConstants.PROP_INSPECTION_DATE)) {
            String dateStr = fields.get(BuildingInspectionConstants.PROP_INSPECTION_DATE).getAsString();
            Date inspectionDate = DateUtil.parseDdMmYyyy(dateStr);
            if (inspectionDate != null) {
                props.putValue(BuildingInspectionConstants.PROP_INSPECTION_DATE, inspectionDate);
                LOGGER.info("  InspectionDate: " + dateStr);
            }
        }

        // BuildingAddress (String) - maps to PropertyAddress
        if (fields.has("BuildingAddress")) {
            String address = fields.get("BuildingAddress").getAsString();
            props.putValue(BuildingInspectionConstants.PROP_PROPERTY_ADDRESS, address);
            LOGGER.info("  PropertyAddress: " + address);
        }

        // BuildingType (String with choice list)
        if (fields.has(BuildingInspectionConstants.PROP_BUILDING_TYPE)) {
            String buildingType = fields.get(BuildingInspectionConstants.PROP_BUILDING_TYPE).getAsString();
            props.putValue(BuildingInspectionConstants.PROP_BUILDING_TYPE, buildingType);
            LOGGER.info("  BuildingType: " + buildingType);
        }

        // InspectorName (String)
        if (fields.has(BuildingInspectionConstants.PROP_INSPECTOR_NAME)) {
            String inspectorName = fields.get(BuildingInspectionConstants.PROP_INSPECTOR_NAME).getAsString();
            props.putValue(BuildingInspectionConstants.PROP_INSPECTOR_NAME, inspectorName);
            LOGGER.info("  InspectorName: " + inspectorName);
        }

        // ComplianceStatus (String with choice list)
        if (fields.has(BuildingInspectionConstants.PROP_COMPLIANCE_STATUS)) {
            String complianceStatus = fields.get(BuildingInspectionConstants.PROP_COMPLIANCE_STATUS).getAsString();
            props.putValue(BuildingInspectionConstants.PROP_COMPLIANCE_STATUS, complianceStatus);
            LOGGER.info("  ComplianceStatus: " + complianceStatus);
        }

        // Municipality (String)
        if (fields.has(BuildingInspectionConstants.PROP_MUNICIPALITY)) {
            String municipality = fields.get(BuildingInspectionConstants.PROP_MUNICIPALITY).getAsString();
            props.putValue(BuildingInspectionConstants.PROP_MUNICIPALITY, municipality);
            LOGGER.info("  Municipality: " + municipality);
        }

    }

}
