package dev.fncm.model;

import java.util.List;

/**
 * Typed result for the building-inspection search operation.
 * Serialised to JSON automatically by Liberty JSON-B.
 */
public record SearchBuildingInspectionResult(
        int count,
        String query,
        List<SearchBuildingInspectionItem> documents) {}

// Made with Bob
