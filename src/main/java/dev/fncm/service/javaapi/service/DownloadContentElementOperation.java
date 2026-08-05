package dev.fncm.service.javaapi.service;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.Id;

import dev.fncm.service.javaapi.FileNetOperation;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Fetches a document by ID, locates the content element whose retrieval name
 * matches the requested name, and returns a streaming JAX-RS {@link Response}
 * with the correct MIME type and a {@code Content-Disposition: attachment} header
 * so the browser triggers a native file download.
 */
public class DownloadContentElementOperation implements FileNetOperation<Response> {

    private static final Logger LOGGER = Logger.getLogger(DownloadContentElementOperation.class.getName());

    private final String documentId;
    private final String retrievalName;

    public DownloadContentElementOperation(String documentId, String retrievalName) {
        this.documentId    = documentId;
        this.retrievalName = retrievalName;
    }

    @Override
    public Response execute(ObjectStore os, String username) throws Exception {
        LOGGER.info("Downloading content element '" + retrievalName + "' from document: " + documentId);

        Document doc = (Document) Factory.Document.fetchInstance(os, new Id(documentId), null);
        ContentElementList elements = doc.get_ContentElements();

        for (int i = 0; i < elements.size(); i++) {
            Object element = elements.get(i);
            if (!(element instanceof ContentTransfer)) {
                continue;
            }
            ContentTransfer ct = (ContentTransfer) element;
            if (!retrievalName.equalsIgnoreCase(ct.get_RetrievalName())) {
                continue;
            }

            String mimeType  = ct.get_ContentType();
            Double size      = ct.get_ContentSize();
            InputStream stream = ct.accessContentStream();

            LOGGER.info("Streaming '" + retrievalName + "' (" + mimeType + ", " + size + " bytes)");

            return Response.ok(stream, mimeType)
                    .header("Content-Disposition", "attachment; filename=\"" + retrievalName + "\"")
                    .header("Content-Length", size)
                    .build();
        }

        throw new IllegalArgumentException(
                "No content element named '" + retrievalName + "' found in document " + documentId);
    }
}

// Made with Bob
