package dev.fncm.model;

/**
 * Request body for POST /api/checkindocument.
 */
public record CheckinDocumentRequest(
        String documentId,
        String reservationId,
        String municipality,
        String propertyAddress,
        String inspectorName,
        String inspectionDate,
        String buildingType,
        String complianceStatus
) {}

// Made with Bob
