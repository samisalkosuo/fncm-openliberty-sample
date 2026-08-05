package dev.fncm.service.graphql;

import java.util.HashMap;
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
 * values (numbers, booleans) are written unquoted.
 */
public class CreateDocumentMutation implements FileUploadOperation {

    private static final Logger LOGGER = Logger.getLogger(CreateBuildingInspectionDocumentOperation.class.getName());

    private static final String FILE_FIELD = "contvar";

    // Query template used when a file is being uploaded.
    // %s is replaced by the optional ", properties:[…]" block.
    private static final String WITH_FILE_TEMPLATE = """
            mutation ($repositoryIdentifier: String!, $classIdentifier: String!,
                      $documentName: String!, $fileInFolderIdentifier: String,
                      $contentType: String, $fileName: String, $contvar: String) {
              createDocument(
                repositoryIdentifier: $repositoryIdentifier
                fileInFolderIdentifier: $fileInFolderIdentifier
                classIdentifier: $classIdentifier
                documentProperties: {
                  name: $documentName
                  %s
                  contentElements: { replace: [{
                    type: CONTENT_TRANSFER
                    contentType: $contentType
                    subContentTransfer: { retrievalName: $fileName content: $contvar }
                  }] }
                }
                checkinAction: {}
              ) { id name }
            }
            """;

    // Query template used when no file is being uploaded.
    // %s is replaced by the optional ", properties:[…]" block.
    private static final String NO_FILE_TEMPLATE = """
            mutation ($repositoryIdentifier: String!, $classIdentifier: String!,
                      $documentName: String!, $fileInFolderIdentifier: String) {
              createDocument(
                repositoryIdentifier: $repositoryIdentifier
                fileInFolderIdentifier: $fileInFolderIdentifier
                classIdentifier: $classIdentifier
                documentProperties: {
                  name: $documentName
                  %s
                }
                checkinAction: {}
              ) { id name }
            }
            """;

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
     *                             {@code "/Folder for Browsing"}); may be
     *                             {@code null}
     * @param classIdentifier      document class name (e.g. {@code "Document"})
     * @param documentName         value for the {@code name} document property
     * @param additionalProperties extra document properties; may be {@code null} or
     *                             empty
     * @param fileBytes            raw bytes of the file to attach; {@code null} for
     *                             no-file path
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
        String propsBlock = buildPropertiesBlock();
        String q = hasFile()
                ? WITH_FILE_TEMPLATE.formatted(propsBlock)
                : NO_FILE_TEMPLATE.formatted(propsBlock);
        LOGGER.info("GraphQL:");
        LOGGER.info(q);
        return q;
    }

    /**
     * Returns all scalar variables. The {@code contvar} entry (set to {@code null}
     * so CP4BA reads file content from the multipart part) is included only when
     * a file is present. {@code fileInFolderIdentifier} is always included; when
     * {@code folderIdentifier} is {@code null} GraphQL treats the omitted optional
     * argument correctly.
     */
    @Override
    public Map<String, Object> variables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("repositoryIdentifier", repositoryIdentifier);
        vars.put("fileInFolderIdentifier", folderIdentifier);
        vars.put("classIdentifier", classIdentifier);
        vars.put("documentName", documentName);
        if (hasFile()) {
            vars.put("contentType", fileContentType);
            vars.put("fileName", fileName);
            vars.put(FILE_FIELD, null);
        }
        return vars;
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
     * Builds the optional {@code , properties:[{Key:"val"}, …]} inline block for
     * {@code additionalProperties}. Returns an empty string when there are no
     * additional properties.
     */
    private String buildPropertiesBlock() {
        if (additionalProperties.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(", properties:[");
        boolean first = true;
        for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            Object val = entry.getValue();
            sb.append("{").append(entry.getKey()).append(": ");
            if (val instanceof String s) {
                sb.append("\"").append(escape(s)).append("\"");
            } else if (val != null) {
                sb.append(val);
            } else {
                sb.append("null");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escapes backslashes and double-quotes for safe inline embedding in a GraphQL
     * string literal (used only for {@code additionalProperties} values).
     */
    private static String escape(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
