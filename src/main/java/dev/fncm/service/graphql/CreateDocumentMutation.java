package dev.fncm.service.graphql;

import java.util.Map;
import java.util.logging.Logger;

import dev.fncm.service.FileUploadOperation;
import dev.fncm.service.javaapi.service.buildinginspectiondocs.CreateBuildingInspectionDocumentOperation;

/**
 * GraphQL {@code createDocument} mutation with optional file content upload.
 *
 * <p>
 * When {@code fileBytes} is non-null the class implements
 * {@link dev.fncm.service.FileUploadOperation}
 * and the file content is uploaded as a separate multipart form part
 * ({@code "contvar"}).
 * When {@code fileBytes} is {@code null} the content block is omitted from the
 * mutation and
 * the plain {@link dev.fncm.service.GraphQLService#execute} path can be used
 * instead.
 *
 * <p>
 * With file (multipart):
 * 
 * <pre>
 * CreateDocumentMutation mutation = new CreateDocumentMutation(
 *         "OS1", "/Folder for Browsing", "Document", "My Report",
 *         Map.of("DocumentTitle", "Q1"),
 *         pdfBytes, "application/pdf", "report.pdf");
 * String result = graphQLService.executeMultipart(mutation, zenToken);
 * </pre>
 *
 * <p>
 * Without file:
 * 
 * <pre>
 * CreateDocumentMutation mutation = new CreateDocumentMutation(
 *         "OS1", "/Folder for Browsing", "Document", "My Report",
 *         Map.of("DocumentTitle", "Q1"),
 *         null, null, null);
 * String result = graphQLService.execute(mutation, zenToken);
 * </pre>
 *
 * <p>
 * Additional document properties are serialised as inline fields inside the
 * {@code documentProperties { … }} block. String values are quoted; non-String
 * values
 * (numbers, booleans) are written unquoted.
 */
public class CreateDocumentMutation implements FileUploadOperation {

    private static final Logger LOGGER = Logger.getLogger(CreateBuildingInspectionDocumentOperation.class.getName());

    private static final String FILE_FIELD = "contvar";

    private final String repositoryIdentifier;
    private final String folderIdentifier;
    private final String classIdentifier;
    private final String documentName;
    private final Map<String, Object> additionalProperties;
    private final byte[] fileBytes;
    private final String fileContentType;
    private final String fileName;

    /**
     * @param repositoryIdentifier object store identifier (e.g. {@code "OS1"})
     * @param folderIdentifier     destination folder path (e.g.
     *                             {@code "/Folder for Browsing"})
     * @param classIdentifier      document class name (e.g. {@code "Document"})
     * @param documentName         value for the {@code name} document property
     * @param additionalProperties extra document properties; may be {@code null} or
     *                             empty
     * @param fileBytes            raw bytes of the file to attach
     * @param fileContentType      MIME type of the file (e.g.
     *                             {@code "application/pdf"})
     * @param fileName             original filename (e.g. {@code "report.pdf"})
     */
    public CreateDocumentMutation(
            String repositoryIdentifier,
            String folderIdentifier,
            String classIdentifier,
            String documentName,
            Map<String, Object> additionalProperties,
            byte[] fileBytes,
            String fileContentType,
            String fileName) {
        this.repositoryIdentifier = repositoryIdentifier;
        this.folderIdentifier = folderIdentifier;
        this.classIdentifier = classIdentifier;
        this.documentName = documentName;
        this.additionalProperties = (additionalProperties != null) ? additionalProperties : Map.of();
        this.fileBytes = fileBytes;
        this.fileContentType = fileContentType;
        this.fileName = fileName;
    }

    /** Returns {@code true} when this mutation carries file content. */
    public boolean hasFile() {
        return fileBytes != null;
    }

    @Override
    public String query() {
        // Build the properties list: [{Key: "value"}, {Key2: "value2"}, ...]
        StringBuilder propertiesList = new StringBuilder();
        for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
            Object val = entry.getValue();
            if (propertiesList.length() > 0) {
                propertiesList.append(", ");
            }
            propertiesList.append("{").append(entry.getKey()).append(": ");
            if (val instanceof String s) {
                propertiesList.append("\"").append(escape(s)).append("\"");
            } else if (val != null) {
                propertiesList.append(val);
            } else {
                propertiesList.append("null");
            }
            propertiesList.append("}");
        }

        StringBuilder sb = new StringBuilder();
        if (hasFile()) {
            sb.append("mutation ($").append(FILE_FIELD).append(":String) {");
        } else {
            sb.append("mutation {");
        }
        sb.append("createDocument(")
                .append("repositoryIdentifier:\"").append(escape(repositoryIdentifier)).append("\" ");
        if (folderIdentifier != null) {
            sb.append("fileInFolderIdentifier:\"").append(escape(folderIdentifier)).append("\" ");

        }
        sb.append("classIdentifier:\"").append(escape(classIdentifier)).append("\" ")
                .append("documentProperties:{")
                .append("name:\"").append(escape(documentName)).append("\"");

        if (propertiesList.length() > 0) {
            sb.append(", properties:[").append(propertiesList).append("]");
        }

        if (hasFile()) {
            sb.append(" contentElements:{replace:[{")
                    .append("type:CONTENT_TRANSFER ")
                    .append("contentType:\"").append(escape(fileContentType)).append("\" ")
                    .append("subContentTransfer:{content:$").append(FILE_FIELD).append("}")
                    .append("}]}");
        }

        sb.append("} ")
                .append("checkinAction:{}")
                .append(") { id name }")
                .append("}");

        LOGGER.info("GraphQL:");
        LOGGER.info(sb.toString());
        return sb.toString();
    }

    /**
     * Returns {@code {"contvar": null}} so CP4BA reads file content from the
     * multipart part.
     * When there is no file ({@link #hasFile()} is false) returns an empty map.
     */
    @Override
    public Map<String, Object> variables() {
        return hasFile()
                ? java.util.Collections.singletonMap(FILE_FIELD, null)
                : Map.of();
    }

    @Override
    public String fileFieldName() {
        return FILE_FIELD;
    }

    @Override
    public byte[] fileBytes() {
        return fileBytes;
    }

    @Override
    public String fileContentType() {
        return fileContentType;
    }

    @Override
    public String fileName() {
        return fileName;
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /**
     * Escapes backslashes and double-quotes for safe inline embedding in a GraphQL
     * string.
     */
    private static String escape(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
