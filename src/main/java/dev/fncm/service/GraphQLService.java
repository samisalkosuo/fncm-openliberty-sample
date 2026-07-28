package dev.fncm.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONObject;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Thin wrapper around {@link GraphQLClient} that accepts {@link GraphQLOperation} instances.
 *
 * <p>This service mirrors the role of {@link dev.fncm.service.javaapi.FileNetService} for JACE
 * operations, giving both back-end channels an identical plug-in shape from the perspective
 * of resource classes.
 *
 * <p>Usage in a resource:
 * <pre>
 *   {@literal @}Inject GraphQLService graphQLService;
 *
 *   public Response list() {
 *       return execute(() -> graphQLService.execute(new ListObjectStoresQuery(), tokenContext.getZenToken()));
 *   }
 * </pre>
 */
@ApplicationScoped
public class GraphQLService {

    private static final Logger LOG = Logger.getLogger(GraphQLService.class.getName());

    @Inject
    GraphQLClient graphQLClient;

    /**
     * Executes a {@link GraphQLOperation} against the configured GraphQL endpoint.
     *
     * @param op       the operation to run (provides query string and optional variables)
     * @param zenToken CP4BA Zen access token from the current session
     * @return raw JSON response string from the GraphQL API
     * @throws GraphQLClient.GraphQLException when the upstream API returns a non-2xx status
     * @throws Exception on network or TLS errors
     */
    public String execute(GraphQLOperation op, String zenToken) throws Exception {
        String jsonBody = buildBody(op);
        LOG.info("GraphQLService executing: " + op.getClass().getSimpleName());
        return graphQLClient.executeJson(jsonBody, zenToken);
    }

    /**
     * Forwards a pre-built GraphQL JSON envelope to the configured endpoint.
     * Use this for proxy/pass-through scenarios where the caller already holds
     * the complete request body (e.g. forwarded from the browser).
     *
     * @param jsonBody complete GraphQL JSON envelope, e.g. {@code {"query":"…","variables":{…}}}
     * @param zenToken CP4BA Zen access token from the current session
     * @return raw JSON response string from the GraphQL API
     * @throws GraphQLClient.GraphQLException when the upstream API returns a non-2xx status
     * @throws Exception on network or TLS errors
     */
    public String executeRaw(String jsonBody, String zenToken) throws Exception {
        LOG.info("GraphQLService executeRaw (proxy)");
        return graphQLClient.executeJson(jsonBody, zenToken);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String buildBody(GraphQLOperation op) {
        JSONObject body = new JSONObject().put("query", op.query());
        Map<String, Object> vars = op.variables();
        if (!vars.isEmpty()) {
            body.put("variables", new JSONObject(vars));
        }
        return body.toString();
    }
}

// Made with Bob
