package dev.fncm.model;

/**
 * Result returned by POST /api/createdocument.
 *
 * <ul>
 *   <li>{@code status}     — "ok" on success, "error" on failure</li>
 *   <li>{@code message}    — human-readable detail</li>
 *   <li>{@code documentId} — FileNet document ID ({@code null} in the placeholder)</li>
 * </ul>
 */
public record CreateDocumentResult(
        String status,
        String message,
        String documentId
) {}

// Made with Bob
