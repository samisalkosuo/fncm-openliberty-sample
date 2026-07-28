package dev.fncm.model;

import java.util.List;

/**
 * Typed result for the list-document-classes operation.
 * Serialised to JSON automatically by Liberty JSON-B.
 */
public record DocumentClassListResult(
        int count,
        List<DocumentClassItem> documentClasses) {}

// Made with Bob
