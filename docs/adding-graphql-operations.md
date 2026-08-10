# Adding GraphQL Operations

This document explains how to add a new typed GraphQL operation on the server side: when to bother, how to implement the `GraphQLOperation` interface, how to expose it through a REST endpoint, and how to call it from the browser. It also explains when to skip the Java layer and use the proxy directly.

---

## Two Approaches

There are two ways to run a GraphQL query in this application:

| Approach | When to use | Where the query lives |
|---|---|---|
| **Browser proxy** | The browser runs the query and displays the result. No server processing needed. | `js/cards/yourCard.js` using `GraphQL.execute(query, variables)` |
| **Typed Java operation** | The server needs to process the GraphQL response (parse it, combine it with JACE data, expose it through a typed REST endpoint). | A class in `service/graphql/` implementing `GraphQLOperation` |

For most new features where you simply want to show a query result in a card, **use the browser proxy** — it requires no Java code. See [GraphQL → Proxy Endpoint](graphql.md#the-proxy-endpoint) and the `GraphQL.execute()` usage in [Frontend](frontend.md).

Only read on if you need the server to process the response.

---

## Anatomy of a Typed Operation

The [`GraphQLOperation`](../src/main/java/dev/fncm/service/GraphQLOperation.java) interface has two methods:

```java
public interface GraphQLOperation {
    String query();                                    // the GraphQL query or mutation string
    default Map<String, Object> variables() { return Map.of(); }  // optional variables
}
```

Existing implementations live in [`service/graphql/`](../src/main/java/dev/fncm/service/graphql/):

| Class | What it does |
|---|---|
| `ListObjectStoresQuery` | Lists all object stores in the domain (no variables) |
| `GetUserGroupsQuery` | Lists all security groups (no variables) |
| `ListDocumentsInFolderQuery` | Lists documents in a folder path (2 variables) |
| `SearchDocumentsQuery` | Searches by class and date filter |
| `CreateDocumentMutation` | Creates a document with metadata |

---

## Step-by-Step: Add a New Typed Operation

### Step 1 — Write the Operation Class

Create a new file in `src/main/java/dev/fncm/service/graphql/`. Name it `<Feature>Query.java` or `<Feature>Mutation.java`.

**Example — query with no variables**:

```java
package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;

/**
 * Returns all folders in the domain (useful for a domain-wide audit).
 */
public class ListAllFoldersQuery implements GraphQLOperation {

    @Override
    public String query() {
        return """
            query ListAllFolders($repositoryIdentifier: String!) {
              folders(repositoryIdentifier: $repositoryIdentifier) {
                folders {
                  id
                  name
                  pathName
                }
              }
            }
            """;
    }

    @Override
    public Map<String, Object> variables() {
        return Map.of("repositoryIdentifier", repositoryIdentifier);
    }

    private final String repositoryIdentifier;

    public ListAllFoldersQuery(String repositoryIdentifier) {
        this.repositoryIdentifier = repositoryIdentifier;
    }
}
```

**Example — query with no variables** (simpler pattern, using `ListObjectStoresQuery` as reference):

```java
package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;

public class GetDomainInfoQuery implements GraphQLOperation {

    @Override
    public String query() {
        return "{ domain { name description } }";
    }
    // No override needed for variables() — default returns Map.of()
}
```

### Step 2 — Create the REST Resource

Create a resource in `src/main/java/dev/fncm/resource/` that injects `GraphQLService` and calls your operation:

```java
package dev.fncm.resource;

import dev.fncm.service.GraphQLClient;
import dev.fncm.service.GraphQLService;
import dev.fncm.service.graphql.GetDomainInfoQuery;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/domaininfo")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class DomainInfoResource extends BaseResource {

    @Inject
    GraphQLService graphQLService;

    @GET
    public Response getDomainInfo() {
        try {
            String responseBody = graphQLService.execute(
                new GetDomainInfoQuery(),
                tokenContext.getZenToken()
            );
            return Response.ok(responseBody).build();
        } catch (GraphQLClient.GraphQLException e) {
            return error(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return error(502, e.getMessage());
        }
    }
}
```

> **Note**: `GraphQLService.execute()` returns the raw JSON string from CP4BA. It is returned as-is to the browser by `Response.ok(responseBody).build()`. There is no deserialization step.

### Step 3 — Add the API Entry

Open `src/main/webapp/js/api.js` and add a new entry to the `API` map:

```js
export const API = {
  // … existing entries …
  domainInfo: '/api/domaininfo',
};
```

### Step 4 — Call from a Card

In your card's `init()` method, call the new endpoint using `apiFetch`:

```js
import { apiFetch, API } from '../api.js';
import { renderJson } from '../util.js';

// inside init():
document.getElementById('domain-info-btn').addEventListener('click', async () => {
  const spinner   = document.getElementById('domain-info-spinner');
  const container = document.getElementById('domain-info-result');
  spinner.classList.remove('hidden');
  container.innerHTML = '';
  try {
    const res = await apiFetch(API.domainInfo);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    renderJson(container, await res.json());
  } catch (err) {
    container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
  } finally {
    spinner.classList.add('hidden');
  }
});
```

---

## Parsing the Response on the Server

If you need the server to parse the GraphQL response (e.g. extract a field and combine it with other data), parse the raw JSON string returned by `graphQLService.execute()`:

```java
import org.json.JSONObject;
import org.json.JSONArray;

@GET
public Response getDomainName() {
    try {
        String raw = graphQLService.execute(new GetDomainInfoQuery(), tokenContext.getZenToken());
        JSONObject json = new JSONObject(raw);
        String name = json.getJSONObject("data")
                          .getJSONObject("domain")
                          .getString("name");
        return Response.ok(new JSONObject().put("domainName", name).toString()).build();
    } catch (GraphQLClient.GraphQLException e) {
        return error(e.getHttpStatus(), e.getMessage());
    } catch (Exception e) {
        return error(500, e.getMessage());
    }
}
```

Use the `org.json` library (already a project dependency — see `pom.xml`) to navigate the response tree.

---

## File Upload Mutations

If your GraphQL mutation creates a document with content (a file), use `FileUploadOperation` and `GraphQLService.executeMultipart()`:

```java
// 1. Implement FileUploadOperation instead of GraphQLOperation:
public class MyUploadMutation implements FileUploadOperation {
    // constructor takes all parameters
    // query() returns the createDocument mutation string
    // variables() returns the variable map
    // fileFieldName() returns "contvar"
    // fileBytes() returns the raw file bytes
    // fileContentType() returns the MIME type
    // fileName() returns the original filename
}

// 2. Call from the resource:
String result = graphQLService.executeMultipart(new MyUploadMutation(…), tokenContext.getZenToken());
```

See [`CreateDocumentMutation`](../src/main/java/dev/fncm/service/graphql/CreateDocumentMutation.java) for a complete example.

---

## When to Use the Proxy Instead

You do not need a typed Java operation when:

- The browser only needs to display the raw GraphQL result.
- No server-side parsing or combining of data is required.
- The query does not need to be triggered by a server-side schedule or event.

In those cases, call `GraphQL.execute()` directly from the card:

```js
const data = await GraphQL.execute(
  `query($repo: String!) { folder(repositoryIdentifier: $repo, identifier: "/") { name } }`,
  { repo: session.config.repositoryIdentifier }
);
renderJson(container, data);
```

This is simpler, faster to implement, and produces the same result for display-only use cases.

---

## Related Documents

- [GraphQL](graphql.md) — proxy endpoint, browser editor, sample query reference
- [Backend](backend.md) — GraphQLService, GraphQLClient, CDI scopes
- [Adding Features](adding-features.md) — scaffold.sh guide, naming conventions
