package dev.fncm.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.json.JSONObject;

import java.util.logging.Logger;

/**
 * Reusable client for the CP4BA Content Services GraphQL API.
 *
 * Usage – inject this bean and call {@link #execute}:
 * <pre>
 *   String result = graphQLClient.execute("{ documents { id name } }");
 * </pre>
 *
 * The caller is responsible for supplying the Zen token (obtained from
 * {@link dev.fncm.auth.TokenContext} in a request-scoped context).
 *
 * Internally the client:
 *   - Wraps the plain query string in a {"query":"…"} JSON envelope
 *   - Generates a random ECM-CS-XSRF-Token UUID per request
 *   - Sends the XSRF token in both the request header and the Cookie header
 *   - Sends the Zen token as Authorization: Bearer
 *   - Respects the {@code tls.certificate.verification.enabled} setting
 *     (delegated to {@link AuthService#httpPostGraphQL})
 */
@ApplicationScoped
public class GraphQLClient {

    private static final Logger LOG = Logger.getLogger(GraphQLClient.class.getName());

    @Inject
    AuthService authService;

    @Inject
    @ConfigProperty(name = "graphql.url")
    String graphqlUrl;

    /**
     * Executes a GraphQL query or mutation against the configured endpoint.
     *
     * @param query    GraphQL query/mutation string, e.g. {@code "{ documents { id name } }"}
     * @param zenToken CP4BA Zen access token from the current session
     * @return raw JSON response string from the GraphQL API
     * @throws GraphQLException when the upstream API returns a non-2xx status
     * @throws java.io.IOException on network or TLS errors
     */
    public String execute(String query, String zenToken) throws Exception {
        return executeJson(buildJsonBody(query), zenToken);
    }

    /**
     * Executes a GraphQL query or mutation with explicit variables.
     *
     * @param query     GraphQL query/mutation string
     * @param variables JSON object string for GraphQL variables, e.g. {@code "{\"id\":\"123\"}"}
     * @param zenToken  CP4BA Zen access token from the current session
     * @return raw JSON response string from the GraphQL API
     */
    public String execute(String query, String variables, String zenToken) throws Exception {
        return executeJson(buildJsonBody(query, variables), zenToken);
    }

    /**
     * Sends a pre-built GraphQL JSON envelope to the configured endpoint.
     * Use this when the caller has already constructed the full request body
     * (e.g. {@code {"query":"…","variables":{…},"operationName":"…"}}).
     *
     * @param jsonBody complete GraphQL JSON request body
     * @param zenToken CP4BA Zen access token from the current session
     * @return raw JSON response string from the GraphQL API
     */
    public String executeJson(String jsonBody, String zenToken) throws Exception {
        LOG.info("GraphQL executeJson → " + graphqlUrl);
        String[] result = authService.httpPostGraphQL(graphqlUrl, zenToken, jsonBody);

        String responseBody = result[0];
        int    status       = Integer.parseInt(result[1]);

        if (status < 200 || status >= 300) {
            throw new GraphQLException(status, responseBody);
        }
        return responseBody;
    }

    // ── JSON body builders ─────────────────────────────────────────────────

    /** Wraps a plain query string: {"query":"…"} */
    private String buildJsonBody(String query) {
        return new JSONObject().put("query", query).toString();
    }

    /** Wraps a query + variables object: {"query":"…","variables":{…}} */
    private String buildJsonBody(String query, String variables) {
        JSONObject body = new JSONObject().put("query", query);
        if (variables != null) {
            body.put("variables", new JSONObject(variables));
        }
        return body.toString();
    }

    // ── Exception ──────────────────────────────────────────────────────────

    /** Thrown when the GraphQL API returns a non-2xx HTTP status. */
    public static class GraphQLException extends Exception {
        private final int httpStatus;

        public GraphQLException(int httpStatus, String body) {
            super("GraphQL API returned HTTP " + httpStatus + ": " + body);
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() { return httpStatus; }
    }
}
