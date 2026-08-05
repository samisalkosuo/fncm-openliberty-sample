package dev.fncm.service.javaapi.service;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.util.Id;

import dev.fncm.model.CheckinDocumentResult;
import dev.fncm.service.javaapi.FileNetOperation;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Fetches a checked-out reservation by ID, updates its custom properties,
 * and checks it in as a new major version.
 *
 * <p>Content elements are preserved automatically — FileNet carries them forward
 * from the reservation when {@code checkin()} is called.
 */
public class CheckinDocumentOperation implements FileNetOperation<CheckinDocumentResult> {

    private static final Logger LOGGER = Logger.getLogger(CheckinDocumentOperation.class.getName());

    private final String documentId;
    private final String reservationId;
    private final String municipality;
    private final String propertyAddress;
    private final String inspectorName;
    private final String inspectionDate;   // ISO-8601: 2026-07-02T00:00:00Z
    private final String buildingType;
    private final String complianceStatus;

    public CheckinDocumentOperation(
            String documentId,
            String reservationId,
            String municipality,
            String propertyAddress,
            String inspectorName,
            String inspectionDate,
            String buildingType,
            String complianceStatus) {
        this.documentId = documentId;
        this.reservationId    = reservationId;
        this.municipality     = municipality;
        this.propertyAddress  = propertyAddress;
        this.inspectorName    = inspectorName;
        this.inspectionDate   = inspectionDate;
        this.buildingType     = buildingType;
        this.complianceStatus = complianceStatus;
    }

    @Override
    public CheckinDocumentResult execute(ObjectStore os, String username) throws Exception {
        LOGGER.info("Fetching reservation: " + reservationId);
        Document reservationDoc = (Document) Factory.Document.fetchInstance(os, new Id(reservationId), null);

        // Update properties on the reservation before checking in
        setProperties(reservationDoc);

        // Check whether content elements were already set on the reservation
        // (e.g. by UpdateContentElementOperation during the edit session).
        // If so, use them as-is. Otherwise copy from the original document.
        ContentElementList contentElements = reservationDoc.get_ContentElements();
        boolean reservationHasContent = contentElements != null && contentElements.size() > 0;

        if (reservationHasContent) {
            LOGGER.info("Reservation already has " + contentElements.size() + " content element(s) — skipping copy from original");
        } else {
            LOGGER.info("Reservation has no content elements — copying from original document: " + documentId);
            if (contentElements == null) {
                contentElements = Factory.ContentElement.createList();
            }

            Document originalDoc = (Document) Factory.Document.fetchInstance(os, new Id(documentId), null);
            ContentElementList originalContentElements = originalDoc.get_ContentElements();

            for (int i = 0; i < originalContentElements.size(); i++) {
                ContentTransfer content = (ContentTransfer) originalContentElements.get(i);
                LOGGER.info("Retrieval name: " + content.get_RetrievalName());
                LOGGER.info("Content size  : " + content.get_ContentSize());
                LOGGER.info("Content type  : " + content.get_ContentType());

                ContentTransfer newContent = Factory.ContentTransfer.createInstance();
                newContent.setCaptureSource(content.accessContentStream());
                newContent.set_RetrievalName(content.get_RetrievalName());
                newContent.set_ContentType(content.get_ContentType());
                contentElements.add(newContent);
            }

            // Reassign updated list to the reservation
            reservationDoc.set_ContentElements(contentElements);
        }
        

        LOGGER.info("Checking in reservation as MAJOR_VERSION");
        reservationDoc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);

        reservationDoc.save(RefreshMode.REFRESH);

        String newId = reservationDoc.get_Id().toString();
        LOGGER.info("Checkin complete. New document ID: " + newId);
        return new CheckinDocumentResult("ok", "Checkin successful", newId);
    }

    private void setProperties(Document doc) throws Exception {
        var props = doc.getProperties();

        props.putValue("Municipality",     municipality);
        props.putValue("PropertyAddress",  propertyAddress);
        props.putValue("InspectorName",    inspectorName);
        props.putValue("BuildingType",     buildingType);
        props.putValue("ComplianceStatus", complianceStatus);

        if (inspectionDate != null && !inspectionDate.isBlank()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = sdf.parse(inspectionDate);
            props.putValue("InspectionDate", parsed);
            LOGGER.info("InspectionDate set to: " + inspectionDate);
        }
    }
}

// Made with Bob
