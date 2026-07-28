package dev.fncm.model;

import java.util.List;

/**
 * Typed result for the list-folders operation.
 * Serialised to JSON automatically by Liberty JSON-B.
 */
public record FolderListResult(
        int count,
        List<FolderItem> folders) {}

// Made with Bob
