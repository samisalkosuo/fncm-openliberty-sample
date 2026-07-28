package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;

import java.util.Map;

/**
 * Searches for documents in an object store by name fragment.
 *
 * <p>Example usage:
 * <pre>
 *   graphQLService.execute(new SearchDocumentsQuery("OS01", "report"), tokenContext.getZenToken())
 * </pre>
 */
public class SearchDocumentsQuery implements GraphQLOperation {

    private final String objectStore;
    private final String searchTerm;

    public SearchDocumentsQuery(String objectStore, String searchTerm) {
        this.objectStore = objectStore;
        this.searchTerm  = searchTerm;
    }

    @Override
    public String query() {
        return """
                query SearchDocuments($objectStore: String!, $searchTerm: String!) {
                  documents(
                    objectStore: $objectStore
                    filter: { name: { contains: $searchTerm } }
                  ) {
                    documents {
                      id
                      name
                      created
                      contentSize
                    }
                  }
                }
                """;
    }

    @Override
    public Map<String, Object> variables() {
        return Map.of(
            "objectStore", objectStore,
            "searchTerm",  searchTerm
        );
    }
}

// Made with Bob
