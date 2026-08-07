# How to Add a New FileNet Operation

This guide explains how to implement a new FileNet JACE operation and wire it into a JAX-RS endpoint in this application.

---

## What is `FileNetOperation<T>`?

[`FileNetOperation<T>`](../../src/main/java/dev/fncm/service/javaapi/FileNetOperation.java) is a plug-in interface with a single method:

```java
T execute(ObjectStore os, String username) throws Exception;
```

`FileNetService.run()` handles everything else: authentication, SSL configuration, the `doAs()` security context, and the connect/disconnect lifecycle. Your class receives a fully authenticated, ready-to-use `ObjectStore` and the resolved username — it only needs to perform the business logic.

| Parameter | What it is |
|-----------|-----------|
| `os` | A connected `ObjectStore` instance scoped to the configured FileNet Content Platform Engine. Use it to fetch, create, update, or delete objects via the JACE API. |
| `username` | The authenticated user's login name, resolved from the bearer token. Useful for setting document properties like `Creator`, or for audit logging. |

**Error signalling:** throw any `Exception` to indicate failure. `FileNetService.run()` propagates it and `BaseResource.execute()` converts it to a JSON error response automatically.

---

## Annotated Template

```java
package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;
import dev.fncm.model.MyResult;           // your result model record/class
import dev.fncm.service.javaapi.FileNetOperation;
import java.util.logging.Logger;

/**
 * One-line description of what this operation does.
 */
public class MyNewOperation implements FileNetOperation<MyResult> {

    private static final Logger LOGGER = Logger.getLogger(MyNewOperation.class.getName());

    // Constructor fields for any input parameters the operation needs.
    // Operations are instantiated per-request; constructor injection is fine.
    private final String someInput;

    public MyNewOperation(String someInput) {
        this.someInput = someInput;
    }

    @Override
    public MyResult execute(ObjectStore os, String username) throws Exception {
        // 1. Log what you are about to do (INFO level).
        LOGGER.info("Starting MyNewOperation for user: " + username);

        // 2. Use os to perform JACE operations.
        //    os.get_Domain().get_Name()       — domain name
        //    os.get_DisplayName()             — object store display name
        //    new SearchSQL(...) / SearchScope — FNSQL queries
        //    Factory.Document.fetchInstance() — fetch a specific document

        // 3. Build and return a result object.
        //    Return type T is whatever the resource needs for JSON serialisation.
        //    If the operation has no meaningful return value, use Void and return null.
        return new MyResult(/* ... */);
    }
}
```

Key decisions:
- **Class name:** describe *what the operation does*, not what it touches — e.g. `CreateDocumentOperation`, `ListFoldersOperation`, `DeleteAllFoldersOperation`.
- **Package:** `dev.fncm.service.javaapi.service` for general operations; put building-inspection-specific classes in the `buildinginspectiondocs` sub-package.
- **Return type `T`:** declare the concrete type your resource needs. Add a model record in `dev.fncm.model` if no suitable type exists. Use `String` only for simple text responses.
- **Logging:** use `java.util.logging.Logger` (not SLF4J or Log4j). Always name the logger after the class itself: `Logger.getLogger(MyNewOperation.class.getName())`.
- **Throwing:** let checked exceptions propagate via `throws Exception`. Do **not** catch-and-swallow inside `execute()` unless you have a genuine recovery strategy.

---

## Step-by-Step Wiring Guide

### Step 1 — Add a result model (if needed)

If no existing model class matches your return data, create a record in `src/main/java/dev/fncm/model/`:

```java
package dev.fncm.model;

public record MyResult(String status, String detail) {}
```

Liberty JSON-B serialises records automatically — no annotations required for simple fields.

### Step 2 — Create the operation class

Create `src/main/java/dev/fncm/service/javaapi/service/MyNewOperation.java` following the template above.

### Step 3 — Create the resource class

Create `src/main/java/dev/fncm/resource/MyNewResource.java`:

```java
package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.MyNewOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/mynewpath")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class MyNewResource extends BaseResource {

    @Inject
    FileNetService fileNetService;

    @GET
    public Response myEndpoint(@QueryParam("input") String input) {
        // Validate required parameters early and return 400 directly.
        if (input == null || input.isBlank()) {
            return error(400, "Query parameter 'input' is required");
        }
        // execute() wraps any exception:
        //   IllegalStateException → 503 Service Unavailable
        //   Any other Exception   → 500 Internal Server Error
        return execute(() -> fileNetService.run(new MyNewOperation(input), tokenContext));
    }
}
```

> **Note:** `tokenContext` is inherited from `BaseResource` — do not redeclare it. `fileNetService.run(operation, tokenContext)` resolves credentials, establishes the JACE connection, and calls `operation.execute(os, username)` inside a `doAs()` security context.

### Step 4 — Verify the endpoint is reachable

The JAX-RS application class automatically picks up any `@Path`-annotated class in the `dev.fncm.resource` package. No registration step is needed. After building and deploying, the endpoint is available at:

```
GET /api/mynewpath?input=value
```

---

## Real Examples in This Codebase

| Use case | Operation class | Resource class |
|----------|----------------|----------------|
| Simple connectivity check | [`ConnectionTestOperation`](../../src/main/java/dev/fncm/service/javaapi/service/ConnectionTestOperation.java) | [`ConnectionTestResource`](../../src/main/java/dev/fncm/resource/ConnectionTestResource.java) |
| Query with result list | [`ListFoldersOperation`](../../src/main/java/dev/fncm/service/javaapi/service/ListFoldersOperation.java) | [`ListFoldersResource`](../../src/main/java/dev/fncm/resource/ListFoldersResource.java) |

---

## Quick Reference

```
FileNetOperation<T>  (interface)
    └── MyNewOperation.java              ← your class
            │
            ▼
FileNetService.run(operation, tokenContext)
            │  ├─ resolves credentials
            │  ├─ configures TLS (SslUtil)
            │  ├─ connects to CPE via JACE
            │  └─ calls operation.execute(os, username) inside doAs()
            ▼
ObjectStore  (ready to use — JACE API)
```
