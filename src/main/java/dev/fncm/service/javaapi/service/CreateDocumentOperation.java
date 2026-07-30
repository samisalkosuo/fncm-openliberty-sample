package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;
import dev.fncm.model.CreateDocumentResult;
import dev.fncm.service.javaapi.FileNetOperation;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Creates a new document in FileNet with the supplied properties and file
 * content.
 *
 * <p>
 * <strong>Placeholder:</strong> {@link #execute} throws
 * {@link UnsupportedOperationException} until the JACE implementation is
 * written.
 * Replace the constant {@link #DOCUMENT_CLASS} with the actual symbolic class
 * name
 * before implementing.
 */
public class CreateDocumentOperation implements FileNetOperation<CreateDocumentResult> {

    private static final Logger LOGGER = Logger.getLogger(CreateDocumentOperation.class.getName());

    // FileNet document class symbolic name
    private static final String DOCUMENT_CLASS = "BuildingInspectionReport";

    private final String municipalityProperty;
    private final String propertyAddressProperty;
    private final String inspectorNameProperty;
    private final Date inspectionDateProperty;
    private final String buildingTypeChoiceListProperty;
    private final String complianceStatusChoiceListProperty;
    private final byte[] fileBytes;
    private final String fileName;

    public CreateDocumentOperation(
            String municipality,
            String propertyAddress,
            String inspectorName,
            Date inspectionDate,
            String buildingType,
            String complianceStatus,
            byte[] fileBytes,
            String fileName) {
        this.municipalityProperty = municipality;
        this.propertyAddressProperty = propertyAddress;
        this.inspectorNameProperty = inspectorName;
        this.inspectionDateProperty = inspectionDate;
        this.buildingTypeChoiceListProperty = buildingType;
        this.complianceStatusChoiceListProperty = complianceStatus;
        this.fileBytes = fileBytes;
        this.fileName = fileName;
    }

    /**
     * Creates the document in FileNet.
     *
     * <p>
     * Implementation steps (to be filled in):
     * <ol>
     * <li>Create a {@code Document} instance for {@link #DOCUMENT_CLASS}</li>
     * <li>Set {@code StringProperty1}, {@code DateProperty1},
     * {@code ChoiceListProperty1}</li>
     * <li>Wrap {@code fileBytes} in an {@code InputStream} and add as a content
     * element</li>
     * <li>Check in and save; return the new document ID in
     * {@link CreateDocumentResult}</li>
     * </ol>
     *
     * @param os       pre-connected ObjectStore (provided by
     *                 {@code FileNetService})
     * @param username authenticated user (provided by {@code FileNetService})
     * @return {@link CreateDocumentResult} with status, message, and documentId
     */
    @Override
    public CreateDocumentResult execute(ObjectStore os, String username) throws Exception {
        LOGGER.info("=================================================");
        LOGGER.info("CreateDocumentOperation — inputs received");
        LOGGER.info("  Document class                   : " + DOCUMENT_CLASS);
        LOGGER.info("  municipalityProperty             : " + municipalityProperty);
        LOGGER.info("  propertyAddressProperty          : " + propertyAddressProperty);
        LOGGER.info("  inspectorNameProperty            : " + inspectorNameProperty);
        LOGGER.info("  inspectionDateProperty           : " + inspectionDateProperty);
        LOGGER.info("  buildingTypeChoiceListProperty   : " + buildingTypeChoiceListProperty);
        LOGGER.info("  complianceStatusChoiceListProperty: " + complianceStatusChoiceListProperty);
        LOGGER.info("  fileName                         : " + fileName);
        LOGGER.info("  fileBytes size                   : " + (fileBytes != null ? fileBytes.length : 0) + " bytes");
        LOGGER.info("=================================================");

        throw new UnsupportedOperationException("CreateDocumentOperation.execute() not yet implemented");
    }
}

// Made with Bob
