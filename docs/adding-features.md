# Adding Features

This document explains how to add a new end-to-end feature to the FNCM OpenLiberty Sample: a backend REST endpoint backed by a FileNet operation, and a frontend card that calls it. The fastest path uses `scaffold.sh`; a manual path is provided for environments without bash (e.g. Windows without WSL).

---

## Naming Conventions

Scaffold.sh derives all file and symbol names from a single **kebab-case slug**:

| Form | Rule | Example |
|---|---|---|
| Slug (input) | kebab-case | `my-inspection` |
| PascalCase (Java) | Capitalize each word | `MyInspection` |
| camelCase (JS) | Lowercase first word | `myInspection` |
| URL path segment | All lowercase | `myinspection` |
| Title Case (UI) | Capitalize each word | `My Inspection` |

---

## Option A — scaffold.sh (recommended)

`scaffold.sh --feature <slug>` generates all artifacts in one step.

### Prerequisites

`scaffold.sh` is a bash script at the repository root. On macOS or Linux it runs natively. On Windows, use Git Bash, WSL, or another bash environment. If you cannot use bash, follow [Option B — Manual Steps](#option-b--manual-steps) instead.

### Run the command

```bash
./scaffold.sh --feature my-inspection
```

**What it creates**:

| File | Description |
|---|---|
| `src/main/java/dev/fncm/resource/MyInspectionResource.java` | JAX-RS endpoint at `GET /api/myinspection` |
| `src/main/java/dev/fncm/model/MyInspectionResult.java` | Java record result type |
| `src/main/java/dev/fncm/service/MyInspectionService.java` | Application-scoped service |
| `src/main/java/dev/fncm/service/javaapi/service/MyInspectionOperation.java` | FileNet JACE operation |
| `src/main/webapp/js/cards/myInspection.js` | Frontend card |

**What it modifies**:

| File | Change |
|---|---|
| `src/main/webapp/js/main.js` | Adds `import './cards/myInspection.js';` (above the marker `// ── Add new card imports above this line ──`) |
| `src/main/webapp/js/api.js` | Adds `myInspection: '/api/myinspection',` to the `API` map |
| `src/main/webapp/js/layout-config.js` | Adds card entry with `row: 99` (placeholder — you must update this to position the card) |

---

## Walking Through the Generated Files

### Result — `MyInspectionResult.java`

```java
package dev.fncm.model;

public record MyInspectionResult(
        String status,
        String message) {}
```

**What to do**: Replace the placeholder fields with the actual data your operation will return. For example:

```java
public record MyInspectionResult(
        String reportId,
        String municipality,
        String status) {}
```

JSON-B serializes this record to JSON automatically — no getters or annotations needed.

### Operation — `MyInspectionOperation.java`

```java
package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;
import dev.fncm.model.MyInspectionResult;
import dev.fncm.service.javaapi.FileNetOperation;

public class MyInspectionOperation implements FileNetOperation<MyInspectionResult> {

    @Override
    public MyInspectionResult execute(ObjectStore os, String username) throws Exception {
        // TODO: implement operation logic
        return new MyInspectionResult("OK", "Operation completed for " + username);
    }
}
```

**What to do**: Replace the TODO body with real JACE code. The `ObjectStore os` is a fully-authenticated, ready-to-use object store. Example:

```java
@Override
public MyInspectionResult execute(ObjectStore os, String username) throws Exception {
    // Example: read a property from a document
    Document doc = Factory.Document.fetchInstance(os, new Id("{SOME-GUID}"), null);
    String title = (String) doc.getProperties().getObjectValue("DocumentTitle");
    return new MyInspectionResult(doc.get_Id().toString(), "Espoo", "Active");
}
```

Refer to the JACE documentation or the existing operations in `src/main/java/dev/fncm/service/javaapi/service/` for examples.

### Service — `MyInspectionService.java`

```java
@ApplicationScoped
public class MyInspectionService {
    @Inject FileNetService fileNetService;

    public MyInspectionResult run(TokenContext tokenContext) throws Exception {
        return fileNetService.run(new MyInspectionOperation(), tokenContext);
    }
}
```

The service is generated but is optional for simple features — many existing resources call `fileNetService.run(new XOperation(), tokenContext)` directly without a separate service class. Use the service when you need orchestration logic that does not belong in the resource (e.g. combining results from two operations).

### Resource — `MyInspectionResource.java`

```java
@Path("/myinspection")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class MyInspectionResource extends BaseResource {
    @Inject FileNetService fileNetService;

    @GET
    public Response myinspection() {
        return execute(() -> fileNetService.run(new MyInspectionOperation(), tokenContext));
    }
}
```

**Common changes**:
- Change `@GET` to `@POST` and add `@Consumes(MediaType.APPLICATION_JSON)` if the endpoint accepts a request body.
- Add `@QueryParam` parameters for GET endpoints:

```java
@GET
public Response myinspection(@QueryParam("folderPath") String folderPath) {
    if (folderPath == null || folderPath.isBlank()) {
        return error(400, "folderPath is required");
    }
    return execute(() -> fileNetService.run(new MyInspectionOperation(folderPath), tokenContext));
}
```

- Update the `MyInspectionOperation` constructor to accept parameters.

### Card — `myInspection.js`

```js
import { apiFetch, API, GraphQL } from '../api.js';
import { esc, renderJson } from '../util.js';
import { session } from '../session.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'my-inspection',
  size: 'normal',
  html: () => `
    <div class="card" id="card-my-inspection">
      <h2>My Inspection</h2>
      <button id="my-inspection-btn">Load</button>
      <div id="my-inspection-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="my-inspection-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('my-inspection-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('my-inspection-spinner');
      const container = document.getElementById('my-inspection-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';
      try {
        const res = await apiFetch(API.myInspection);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        renderJson(container, await res.json());
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});
```

**Common changes**:
- Update `size` if the card needs more space (`'wide'`, `'large'`, `'full'`).
- Replace `renderJson(container, data)` with `renderWithToggle(container, data, renderer)` for a custom table/list view.
- Add form inputs in `html()` and read them in `init()`.

---

## Checklist: Scaffold to Running Feature

```
[ ] Run: ./scaffold.sh --feature my-inspection
[ ] Edit MyInspectionResult.java  — add real data fields
[ ] Edit MyInspectionOperation.java — implement FileNet logic
[ ] Edit MyInspectionResource.java — add query/body params if needed
[ ] Edit myInspection.js  — customise card HTML and rendering
[ ] Edit layout-config.js — set row and column for 'my-inspection' (currently row: 99 is a placeholder)
[ ] Run: mvn package && mvn liberty:run  (or docker build + run)
[ ] Open http://localhost:9080, log in, find the card, click Load
```

---

## Option B — Manual Steps

Follow this section if you cannot run bash scripts (e.g. Windows without WSL).

Use `my-inspection` as the example slug. Substitute your own slug throughout.

**Naming reference**:

| Form | Value |
|---|---|
| Slug | `my-inspection` |
| PascalCase | `MyInspection` |
| camelCase | `myInspection` |
| URL path | `/api/myinspection` |

### Step 1 — Create the Result record

Create `src/main/java/dev/fncm/model/MyInspectionResult.java`:

```java
package dev.fncm.model;

public record MyInspectionResult(
        String status,
        String message) {}
```

### Step 2 — Create the Operation

Create `src/main/java/dev/fncm/service/javaapi/service/MyInspectionOperation.java`:

```java
package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;
import dev.fncm.model.MyInspectionResult;
import dev.fncm.service.javaapi.FileNetOperation;
import java.util.logging.Logger;

public class MyInspectionOperation implements FileNetOperation<MyInspectionResult> {
    private static final Logger LOGGER = Logger.getLogger(MyInspectionOperation.class.getName());

    @Override
    public MyInspectionResult execute(ObjectStore os, String username) throws Exception {
        // implement your FileNet logic here
        return new MyInspectionResult("OK", "Operation completed for " + username);
    }
}
```

### Step 3 — Create the Resource

Create `src/main/java/dev/fncm/resource/MyInspectionResource.java`:

```java
package dev.fncm.resource;

import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.MyInspectionOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/myinspection")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class MyInspectionResource extends BaseResource {
    @Inject FileNetService fileNetService;

    @GET
    public Response myinspection() {
        return execute(() -> fileNetService.run(new MyInspectionOperation(), tokenContext));
    }
}
```

### Step 4 — Create the JS Card

Create `src/main/webapp/js/cards/myInspection.js`. Copy the contents from [`scaffold/templates/card/_template.js`](../scaffold/templates/card/_template.js) and replace every occurrence of `my-feature` with `my-inspection` and `My Feature` with `My Inspection`.

Also replace `API.myEndpoint` with `API.myInspection`.

### Step 5 — Register the Card Import in main.js

Open `src/main/webapp/js/main.js` and add this line **before** the marker comment `// ── Add new card imports above this line ──`:

```js
import './cards/myInspection.js';
```

### Step 6 — Add the API Entry in api.js

Open `src/main/webapp/js/api.js` and add this line to the `API` object (before the closing `};`):

```js
myInspection:              '/api/myinspection',
```

### Step 7 — Add Card Entry in layout-config.js

Open `src/main/webapp/js/layout-config.js` and add this entry to the `layoutConfig.cards` object (before the closing `},`):

```js
'my-inspection': {
  row: 1,           // FIXME: Update row/column to position this card in the grid
  column: 1,
  size: 'normal',
},
```

**Important**: Update `row` and `column` to position the card in your grid. See [Layout Configuration in Frontend documentation](frontend.md#layout-configuration) for details.

---

## Secondary Scaffold Commands

### Card Only

Generate a JS card without any Java files:

```bash
./scaffold.sh --card my-inspection
```

Use this when you only need a new UI panel that calls an existing endpoint or the GraphQL proxy.

### Java Only

Generate the four Java files without a JS card:

```bash
./scaffold.sh --java MyInspection
```

Use this when building a backend endpoint you plan to wire to an existing card, or when using a non-card frontend pattern.

### CSS Themes

List available themes:

```bash
./scaffold.sh --css list
```

Apply a theme (backs up current CSS automatically):

```bash
./scaffold.sh --css frost
```

Restore from backup by copying any set from `scaffold/backups/css-TIMESTAMP/` back to `src/main/webapp/css/`.

**Add a custom theme**: create a directory under `scaffold/templates/css/my-theme/` containing all five CSS files (`app.css`, `tokens.css`, `layout.css`, `card.css`, `components.css`), then run `./scaffold.sh --css my-theme`.

### Combine Commands

Multiple options can be combined in one call:

```bash
./scaffold.sh --java MyInspection --card my-inspection --css frost
```

---

## Removing a Feature

`--remove-feature` is the exact inverse of `--feature`. It deletes all four Java files, the JS card file, and removes:

- The import from `src/main/webapp/js/main.js`
- The API entry from `src/main/webapp/js/api.js`
- The card entry from `src/main/webapp/js/layout-config.js`

```bash
./scaffold.sh --remove-feature my-inspection
```

Files that don't exist are silently skipped with a warning.

---

## Related Documents

- [Backend](backend.md) — vertical-slice pattern, BaseResource, FileNetService
- [Frontend](frontend.md) — card system, session.js, API module
- [Adding GraphQL Operations](adding-graphql-operations.md) — GraphQL-specific guide
- [Architecture](architecture.md) — overall system structure
