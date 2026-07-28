package dev.fncm.model;

/**
 * A single document-class entry returned by the list-document-classes operation.
 * Serialised to JSON automatically by Liberty JSON-B.
 */
public record DocumentClassItem(
        String symbolicName,
        String displayName,
        String description,
        String type) {}

// Made with Bob
