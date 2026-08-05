package dev.fncm.model;

/**
 * Result returned by POST /api/checkindocument.
 *
 * <ul>
 *   <li>{@code status}     — "ok" on success, "error" on failure</li>
 *   <li>{@code message}    — human-readable detail</li>
 *   <li>{@code documentId} — FileNet ID of the newly checked-in document version</li>
 * </ul>
 */
public record CheckinDocumentResult(
        String status,
        String message,
        String documentId
) {}

// Made with Bob
