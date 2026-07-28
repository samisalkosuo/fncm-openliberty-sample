package dev.fncm.model;

/**
 * A single folder entry returned by the list-folders operation.
 * Serialised to JSON automatically by Liberty JSON-B.
 */
public record FolderItem(
        String path,
        String id,
        String created,
        String creator) {}

// Made with Bob
