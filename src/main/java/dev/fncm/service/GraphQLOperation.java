package dev.fncm.service;

import java.util.Map;

/**
 * Plug-in contract for a single GraphQL operation.
 * Implement this interface and pass an instance to {@link GraphQLService#execute} —
 * XSRF token, authorization header, TLS, and HTTP are all handled by the service.
 *
 * <p>Minimum implementation: override {@link #query()} only.
 * Variables are optional — the default returns an empty map.
 *
 * <pre>
 * public class MyQuery implements GraphQLOperation {
 *     {@literal @}Override
 *     public String query() {
 *         return "{ myEntity { id name } }";
 *     }
 * }
 * </pre>
 */
public interface GraphQLOperation {

    /** GraphQL query or mutation string to execute. */
    String query();

    /**
     * GraphQL variables to send alongside the query.
     * Returns an empty map by default; override to supply variables.
     */
    default Map<String, Object> variables() {
        return Map.of();
    }
}

// Made with Bob
