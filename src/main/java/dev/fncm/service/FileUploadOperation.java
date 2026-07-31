package dev.fncm.service;

/**
 * Extension of {@link GraphQLOperation} for mutations that upload a file as a
 * multipart form part alongside the GraphQL JSON envelope.
 *
 * <p>The multipart body sent to CP4BA has two parts:
 * <ol>
 *   <li>{@code graphql} — {@code application/json} — the normal GraphQL envelope
 *       ({@code {"query":"…","variables":{…}}})</li>
 *   <li>{@link #fileFieldName()} — {@link #fileContentType()} — raw file bytes,
 *       referenced by the GraphQL variable of the same name</li>
 * </ol>
 *
 * <p>Implement this interface instead of plain {@link GraphQLOperation} whenever
 * the mutation needs to stream file content to the server (e.g. {@code createDocument}).
 *
 * <p>Usage:
 * <pre>
 *   FileUploadOperation op = new CreateDocumentMutation(...);
 *   String result = graphQLService.executeMultipart(op, zenToken);
 * </pre>
 */
public interface FileUploadOperation extends GraphQLOperation {

    /**
     * Name of the multipart form field that carries the file bytes.
     * Must match the GraphQL variable name used in {@link #query()}.
     * Example: {@code "contvar"}
     */
    String fileFieldName();

    /** Raw bytes of the file to upload. */
    byte[] fileBytes();

    /**
     * MIME type of the file, used as the {@code Content-Type} of the file part.
     * Example: {@code "application/pdf"}, {@code "text/plain"}
     */
    String fileContentType();

    /**
     * Original filename, used in the {@code Content-Disposition: form-data; filename="…"} header
     * of the file part.
     */
    String fileName();
}
