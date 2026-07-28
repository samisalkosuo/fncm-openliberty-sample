package dev.fncm.model;

/**
 * Typed result for a FileNet connection test.
 * Serialised to JSON automatically by Liberty JSON-B.
 */
public record ConnectionTestResult(
        String status,
        String domain,
        String objectStore,
        String user) {}

// Made with Bob
