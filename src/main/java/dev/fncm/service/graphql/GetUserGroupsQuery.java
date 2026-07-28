package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;

public class GetUserGroupsQuery implements GraphQLOperation {

    @Override
    public String query() {
        return """
                {
                 secGroups(
                   realmIdentifier:null
                   searchPattern:""
                   searchType:NONE
                   searchAttribute:SHORT_NAME
                   sortType:ASCENDING
                 )
                 {
                    groups{
                     id
                     shortName
                     displayName
                     distinguishedName
                   }
                 }
                }                """;
    }
}
