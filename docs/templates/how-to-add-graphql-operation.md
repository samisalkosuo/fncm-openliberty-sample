# How to Add a New GraphQL Operation

This guide explains how to implement a new GraphQL query or mutation and wire it into a JAX-RS endpoint in this application.

---

## What is `GraphQLOperation`?

[`GraphQLOperation`](../../src/main/java/dev/fncm/service/GraphQLOperation.java) is a plug-in interface with two methods:

| Method | Required? | Purpose |
|--------|-----------|---------|
| `query()` | **Yes** | Returns the GraphQL query or mutation string |
| `variables()` | No (default: empty map) | Returns a `Map<String, Object>` of GraphQL variables |

`GraphQLService.execute()` handles everything else: the XSRF token, the `Authorization` header, TLS, and the HTTP call. Your class only needs to know *what* to ask for.

**When to use `GraphQLOperation` vs raw `executeRaw()`:** Use `GraphQLOperation` for all structured queries and mutations. `executeRaw()` is only needed when you must pass a fully-formed JSON body yourself (e.g. proxying an external client request verbatim — see `GraphQLProxyResource`).

---

## Annotated Template

```java
package dev.fncm.service.graphql;

import dev.fncm.service.GraphQLOperation;
import java.util.Map;

public class MyNewQuery implements GraphQLOperation {

    // Fields to capture constructor parameters used as query variables.
    // If the query takes no parameters, omit the constructor entirely.
    private final String repositoryIdentifier;
    private final String folderPath;

    public MyNewQuery(String repositoryIdentifier, String folderPath) {
        this.repositoryIdentifier = repositoryIdentifier;
        this.folderPath           = folderPath;
    }

    @Override
    public String query() {
        // Use a Java text block (Java 15+) for multi-line queries.
        // The variable names here must match the keys in variables() below.
        return """
                query MyNewQuery($repositoryIdentifier: String!, $folderPath: String!) {
                  folder(
                    repositoryIdentifier: $repositoryIdentifier
                    identifier: $folderPath
                  ) {
                    id
                    name
                  }
                }
                """;
    }

    // Only override variables() when the query uses $ parameters.
    // If the query is static (no parameters), delete this method — the
    // default in GraphQLOperation returns an empty map automatically.
    @Override
    public Map<String, Object> variables() {
        return Map.of(
            "repositoryIdentifier", repositoryIdentifier,
            "folderPath",           folderPath
        );
    }
}
```

Key decisions:
- **Class name and file name must match** (Java requirement). Use the suffix `Query` for queries and `Mutation` for mutations.
- **Package:** `dev.fncm.service.graphql` — all operation classes live here.
- **`query()` returns a `String`** containing the full GraphQL document. Paste the query exactly as you would send it in GraphQL Playground.
- **Variable keys in `variables()`** must be identical to the `$variable` names declared in the query signature.

---

## Step-by-Step Wiring Guide

### Step 1 — Create the operation class

Create `src/main/java/dev/fncm/service/graphql/MyNewQuery.java` following the template above.

- Queries that need no input: omit the constructor and `variables()`.
- Queries that require input: add constructor fields and override `variables()`.

### Step 2 — Create the resource class

Create `src/main/java/dev/fncm/resource/MyNewResource.java`:

```java
package dev.fncm.resource;

import dev.fncm.service.GraphQLClient;
import dev.fncm.service.GraphQLService;
import dev.fncm.service.graphql.MyNewQuery;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/mynewpath")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class MyNewResource extends BaseResource {

    @Inject
    GraphQLService graphQLService;

    // Inject config properties if needed (e.g. the object store identifier).
    @Inject
    @ConfigProperty(name = "filenet.objectstore")
    String objectStore;

    @GET
    public Response myEndpoint(@QueryParam("folder") String folder) {
        // Validate required parameters early and return 400 directly.
        if (folder == null || folder.isBlank()) {
            return error(400, "Query parameter 'folder' is required");
        }
        try {
            String responseBody = graphQLService.execute(
                    new MyNewQuery(objectStore, folder),
                    tokenContext.getZenToken());
            return Response.ok(responseBody).build();
        } catch (GraphQLClient.GraphQLException e) {
            // GraphQL-level errors carry an HTTP status from the server.
            return error(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            return error(502, e.getMessage());
        }
    }
}
```

> **Note:** The `tokenContext` field is inherited from `BaseResource` — do not redeclare it.

### Step 3 — Verify the endpoint is reachable

The JAX-RS application class automatically picks up any `@Path`-annotated class in the `dev.fncm.resource` package. No registration step is needed. After building and deploying, the endpoint is available at:

```
GET /api/mynewpath
```

---

## Real Examples in This Codebase

| Use case | Operation class | Resource class |
|----------|----------------|----------------|
| No-variable query | [`GetUserGroupsQuery`](../../src/main/java/dev/fncm/service/graphql/GetUserGroupsQuery.java) | [`GetUserGroupsResource`](../../src/main/java/dev/fncm/resource/GetUserGroupsResource.java) |
| Query with variables | [`ListDocumentsInFolderQuery`](../../src/main/java/dev/fncm/service/graphql/ListDocumentsInFolderQuery.java) | [`ListDocumentsInFolderResource`](../../src/main/java/dev/fncm/resource/ListDocumentsInFolderResource.java) |

---

## Quick Reference

```
GraphQLOperation  (interface)
    └── MyNewQuery.java              ← your class
            │
            ▼
GraphQLService.execute(operation, zenToken)
            │
            ▼
GraphQLClient  (HTTP + auth — do not touch)
```
