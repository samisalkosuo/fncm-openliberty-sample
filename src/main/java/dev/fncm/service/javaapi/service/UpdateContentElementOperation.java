package dev.fncm.service.javaapi.service;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.Id;

import dev.fncm.model.ResponseMessage;
import dev.fncm.service.javaapi.FileNetOperation;

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

/**
 * Adds a new content element to, or replaces all content elements on, a
 * checked-out reservation document — without checking it in.
 *
 * <p>Mode values:
 * <ul>
 *   <li>{@code "add"}     — appends the new file to the existing content elements.</li>
 *   <li>{@code "replace"} — discards all existing content elements and sets only the new file.</li>
 * </ul>
 */
public class UpdateContentElementOperation implements FileNetOperation<ResponseMessage> {

    private static final Logger LOGGER = Logger.getLogger(UpdateContentElementOperation.class.getName());

    private final String documentId;
    private final String reservationId;
    private final String mode;
    private final byte[] fileBytes;
    private final String fileName;
    private final String contentType;

    public UpdateContentElementOperation(
            String documentId,
            String reservationId,
            String mode,
            byte[] fileBytes,
            String fileName,
            String contentType) {
        this.documentId    = documentId;
        this.reservationId = reservationId;
        this.mode          = mode;
        this.fileBytes     = fileBytes;
        this.fileName      = fileName;
        this.contentType   = contentType;
    }

    @Override
    public ResponseMessage execute(ObjectStore os, String username) throws Exception {
        LOGGER.info("Fetching reservation: " + reservationId + " (mode=" + mode + ")");
        Document reservationDoc = (Document) Factory.Document.fetchInstance(os, new Id(reservationId), null);

        
        ContentElementList list = Factory.ContentElement.createList();
        if ("replace".equals(mode)) {
            LOGGER.info("Replace mode — creating empty content element list");
            //list = Factory.ContentElement.createList();
        } else {
            LOGGER.info("Add mode — using existing content elements");
            
            Document originalDoc = (Document) Factory.Document.fetchInstance(os, new Id(documentId), null);
            ContentElementList originalContentList = originalDoc.get_ContentElements();

            for (int i = 0; i < originalContentList.size(); i++) {
                ContentTransfer content = (ContentTransfer) originalContentList.get(i);

                ContentTransfer newContent = Factory.ContentTransfer.createInstance();
                newContent.setCaptureSource(content.accessContentStream());
                newContent.set_RetrievalName(content.get_RetrievalName());
                newContent.set_ContentType(content.get_ContentType());
                list.add(newContent);
            }

            /*
            list = reservationDoc.get_ContentElements();
            if (list == null) {
                list = Factory.ContentElement.createList();
            }
            */
        }

        ContentTransfer ct = Factory.ContentTransfer.createInstance();
        ct.setCaptureSource(new ByteArrayInputStream(fileBytes));
        ct.set_RetrievalName(fileName);
        ct.set_ContentType(contentType);
        list.add(ct);

        reservationDoc.set_ContentElements(list);
        reservationDoc.save(RefreshMode.NO_REFRESH);

        LOGGER.info("Content element updated on reservation: " + reservationId);
        return new ResponseMessage("Content element updated");
    }
}

// Made with Bob
