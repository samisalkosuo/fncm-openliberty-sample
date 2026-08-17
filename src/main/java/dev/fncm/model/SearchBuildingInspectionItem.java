package dev.fncm.model;

/**
 * A single hit returned by the building-inspection search operation.
 * Serialised to JSON automatically by Liberty JSON-B.
 */
public record SearchBuildingInspectionItem(
        String id,
        String documentTitle,
        String municipality,
        String propertyAddress,
        String inspectorName,
        String buildingType,
        String complianceStatus,
        String inspectionDate,
        String dateCreated) {}

// Made with Bob
