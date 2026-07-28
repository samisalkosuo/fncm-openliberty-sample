package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;

/**
 * Lists all object stores available in the connected CP4BA domain.
 *
 * <p>Inject {@link dev.fncm.service.GraphQLService} in your resource and call:
 * <pre>
 *   graphQLService.execute(new ListObjectStoresQuery(), tokenContext.getZenToken())
 * </pre>
 */
public class ListObjectStoresQuery implements GraphQLOperation {

    @Override
    public String query() {
        return """
                {
                  objectStores {
                    connection {
                      objectStores {
                        displayName
                        symbolicName
                        description
                      }
                    }
                  }
                }
                """;
    }
}

// Made with Bob
