package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;

/**
 * _QueryTemplate — copy and rename this file to implement a new GraphQL operation.
 *
 * Steps:
 *  1. Rename this class to {@code MyQuery} and this file to {@code MyQuery.java}.
 *  2. Replace the placeholder query string in {@link #query()} with your real query.
 *  3. Optionally override {@link #variables()} if the query requires variables.
 *  4. In your resource, inject {@link dev.fncm.service.GraphQLService} and call:
 *     <pre>graphQLService.execute(new MyQuery(), tokenContext.getZenToken())</pre>
 */
public class _QueryTemplate implements GraphQLOperation {

    @Override
    public String query() {
        // Replace this placeholder with your GraphQL query or mutation.
        // Use a text block (Java 15+) for multi-line queries.
        return "{ __typename }";
    }

    // Uncomment and override variables() if the query requires input parameters:
    //
    // @Override
    // public Map<String, Object> variables() {
    //     return Map.of("myParam", myValue);
    // }
}

// Made with Bob
