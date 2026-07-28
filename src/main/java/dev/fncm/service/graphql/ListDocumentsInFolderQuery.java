package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;

import java.util.Map;

/**
 * Lists documents and sub-folders inside a given folder path.
 *
 * <p>Example usage:
 * <pre>
 *   graphQLService.execute(new ListDocumentsInFolderQuery("OS1", "/BuildingInspectionReports/ByDate/2025/04"), zenToken)
 * </pre>
 */
public class ListDocumentsInFolderQuery implements GraphQLOperation {

    private final String repositoryIdentifier;
    private final String folderPath;

    public ListDocumentsInFolderQuery(String repositoryIdentifier, String folderPath) {
        this.repositoryIdentifier = repositoryIdentifier;
        this.folderPath           = folderPath;
    }

    @Override
    public String query() {
        return """
                query ListDocuments($repositoryIdentifier: String!, $folderPath: String!) {
                  folder(
                    repositoryIdentifier: $repositoryIdentifier
                    identifier: $folderPath
                  ) {
                    className
                    id
                    name
                    pathName
                    subFolders { folders { name } }
                    containedDocuments {
                      documents {
                        id
                        name
                        className
                        dateCreated
                      }
                    }
                  }
                }
                """;
    }

    @Override
    public Map<String, Object> variables() {
        return Map.of(
            "repositoryIdentifier", repositoryIdentifier,
            "folderPath",           folderPath
        );
    }
}

// Made with Bob
