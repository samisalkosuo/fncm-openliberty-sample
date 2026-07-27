# Refactoring Plan — filenet-openliberty

This document captures all agreed refactoring suggestions for making the application
extensible and production-grade as an end-user-facing FileNet solution.
Reference this document when asking Bob to implement any item.

---

## R1 — `FileNetService` + `FileNetOperation<T>` Interface

**Goal:** Replace the pattern of directly `new`-ing JACE classes inside resource methods with
a plug-in model. Adding new FileNet functionality becomes: write one operation class, done.

### Current problem
Resources call `new ConnectionTest(username, zenToken)` and `new ListFolders(username, zenToken)`
directly. Every new feature requires a new resource class *and* a new JACE class with no shared
contract or lifecycle control.

### Target structure

```
dev.fncm.service.javaapi/
├── FileNetOperation<T>          (interface)
│     └── T execute(ObjectStore os, String username) throws Exception
├── FileNetService               (@ApplicationScoped CDI bean)
│     └── <T> T run(FileNetOperation<T> op, TokenContext ctx)
│           — owns: config load, SSL, connect, credentials.doAs(), disconnect
└── service/
      ├── ConnectionTestOperation   implements FileNetOperation<ConnectionTestResult>
      ├── ListFoldersOperation      implements FileNetOperation<FolderListResult>
      └── _OperationTemplate.java   (copy to add new operations)
```

### How to add a new JACE feature after this refactor
1. Create `MyOperation implements FileNetOperation<MyResult>` — implement `execute()`.
2. Inject `FileNetService` into your resource and call `service.run(new MyOperation(), tokenContext)`.
3. No changes to auth, config, SSL, or lifecycle code.

### Files changed
- `src/main/java/dev/fncm/service/javaapi/FileNetOperation.java` — **new**
- `src/main/java/dev/fncm/service/javaapi/FileNetService.java` — **new** (replaces `BaseFileNetApp`)
- `src/main/java/dev/fncm/service/javaapi/service/ConnectionTestOperation.java` — **new**
- `src/main/java/dev/fncm/service/javaapi/service/ListFoldersOperation.java` — **new**
- `src/main/java/dev/fncm/resource/ConnectionTestResource.java` — simplified
- `src/main/java/dev/fncm/resource/ListFoldersResource.java` — simplified
- `src/main/java/dev/fncm/service/javaapi/BaseFileNetApp.java` — **deleted** (superseded)
- `src/main/java/dev/fncm/service/javaapi/service/ConnectionTest.java` — **deleted**
- `src/main/java/dev/fncm/service/javaapi/service/ListFolders.java` — **deleted**

---

## R2 — Split JavaScript into ES Modules

**Goal:** Replace the single 450-line inline `<script>` block with a structured module tree.
Each card is an isolated file; adding a new card = add one file.

### Target structure

```
src/main/webapp/
├── index.html              (HTML skeleton + <script type="module" src="js/main.js">)
├── css/
│   └── app.css             (extracted from <style> block in index.html)
└── js/
    ├── main.js             (imports + wires all cards on DOMContentLoaded)
    ├── session.js          (save / load / clear)
    ├── router.js           (showView, enterApp, logout)
    ├── api.js              (apiFetch — central auth header, base URLs as constants)
    └── cards/
        ├── _template.js    (copy this to add a new card)
        ├── graphql.js
        ├── connectionTest.js
        ├── listFolders.js
        └── documents.js
```

### `api.js` contract

```javascript
// All fetch calls go through here — auth header attached once, everywhere.
export async function apiFetch(url, options = {}) { ... }
export const API = {
  login:          '/api/auth/login',
  graphql:        '/api/graphql',
  connectionTest: '/api/connectiontest',
  listFolders:    '/api/listfolders',
  documents:      '/api/documents',
};
```

### `_template.js` contract

```javascript
// cards/_template.js — copy and rename for each new card
export function init() {
  document.getElementById('my-btn').addEventListener('click', async () => {
    // 1. show spinner
    // 2. call apiFetch(API.myEndpoint)
    // 3. render result
    // 4. hide spinner
  });
}
```

### How to add a new card after this refactor
1. Add the `<div class="card">` block in `index.html`.
2. Copy `_template.js` → `cards/myFeature.js`, implement the click handler.
3. Import and call `init()` from `main.js`.
4. No other files change.

### Files changed
- `src/main/webapp/index.html` — stripped to HTML skeleton only
- `src/main/webapp/css/app.css` — **new** (extracted CSS)
- `src/main/webapp/js/main.js` — **new**
- `src/main/webapp/js/session.js` — **new**
- `src/main/webapp/js/router.js` — **new**
- `src/main/webapp/js/api.js` — **new**
- `src/main/webapp/js/cards/graphql.js` — **new**
- `src/main/webapp/js/cards/connectionTest.js` — **new**
- `src/main/webapp/js/cards/listFolders.js` — **new**
- `src/main/webapp/js/cards/documents.js` — **new**
- `src/main/webapp/js/cards/_template.js` — **new**

---

## R3 — `BaseResource` with Uniform Error Handling

**Goal:** Eliminate copy-paste boilerplate across resource classes and ensure all errors
return a consistent JSON shape.

### Current problem
Every resource repeats: inject `TokenContext`, inject `TokenCache`, try/catch, build
`Response`. `ListFoldersResource` already contains a copy-paste bug (uses
`ConnectionTestResource`'s logger class name).

### Target pattern

```java
// dev.fncm.resource.BaseResource
public abstract class BaseResource {
    @Inject protected TokenContext tokenContext;
    @Inject protected TokenCache   tokenCache;

    protected <T> Response execute(Supplier<T> action) {
        try {
            return Response.ok(action.get()).build();
        } catch (IllegalStateException e) {
            return error(503, e.getMessage());
        } catch (Exception e) {
            return error(500, e.getMessage());
        }
    }

    protected Response error(int status, String message) {
        return Response.status(status)
            .entity(new JSONObject().put("error", message).toString())
            .build();
    }
}
```

A complete new resource after this refactor:

```java
@Path("/folders")
public class ListFoldersResource extends BaseResource {
    @Inject FileNetService fileNetService;

    @GET
    public Response list() {
        return execute(() -> fileNetService.run(new ListFoldersOperation(), tokenContext));
    }
}
```

### Files changed
- `src/main/java/dev/fncm/resource/BaseResource.java` — **new**
- `src/main/java/dev/fncm/resource/AuthResource.java` — extend `BaseResource`
- `src/main/java/dev/fncm/resource/ConnectionTestResource.java` — extend `BaseResource`
- `src/main/java/dev/fncm/resource/ListFoldersResource.java` — extend `BaseResource`
- `src/main/java/dev/fncm/resource/DocumentResource.java` — extend `BaseResource`
- `src/main/java/dev/fncm/resource/GraphQLProxyResource.java` — extend `BaseResource`

---

## R4 — `GraphQLOperation` Interface (Symmetric with R1)

**Goal:** Give GraphQL calls the same plug-in shape as JACE calls so both back-end channels
look identical to resource code.

### Target structure

```
dev.fncm.service/
├── GraphQLOperation          (interface or record)
│     ├── String query()
│     └── default Map<String,Object> variables()  { return Map.of(); }
├── GraphQLService            (thin wrapper around GraphQLClient — @ApplicationScoped)
│     └── JSONObject execute(GraphQLOperation op, String zenToken)
└── graphql/
      ├── ListObjectStoresQuery    implements GraphQLOperation
      ├── SearchDocumentsQuery     implements GraphQLOperation
      └── _QueryTemplate.java      (copy to add new queries)
```

### How to add a new GraphQL feature after this refactor
1. Create `MyQuery implements GraphQLOperation` — implement `query()`.
2. Inject `GraphQLService` into your resource and call `service.execute(new MyQuery(), tokenContext.getZenToken())`.

### Files changed
- `src/main/java/dev/fncm/service/GraphQLOperation.java` — **new**
- `src/main/java/dev/fncm/service/GraphQLService.java` — **new** (thin wrapper)
- `src/main/java/dev/fncm/service/graphql/ListObjectStoresQuery.java` — **new** (example)
- `src/main/java/dev/fncm/service/graphql/_QueryTemplate.java` — **new**
- `src/main/java/dev/fncm/resource/GraphQLProxyResource.java` — updated to use `GraphQLService`

---

## R5 — Unify Configuration into MicroProfile Config

**Goal:** Remove the two overlapping config files and the custom `FileNetConfig` file-loading
code. Use MicroProfile Config's built-in environment-variable override for all settings.

### Current problem
- `microprofile-config.properties` — `iam.host`, `cp4ba.host`, `external.api.base.url`
- `filenet-cli.properties` — `filenet.iamhost`, `filenet.cp4bahost`, `filenet.url`, `filenet.objectstore`

IAM and CP4BA host appear in **both** files under different key names.
`FileNetConfig` has its own `getConfigValue()` / `cleanValue()` loading code that bypasses
MicroProfile Config entirely.

### Target `microprofile-config.properties`

```properties
# CP4BA auth
iam.host=https://...
cp4ba.host=https://...

# FileNet CPE
filenet.cpe.url=https://.../cpe/wsi/FNCEWS40MTOM/
filenet.domain=P8DOMAIN
filenet.objectstore=OS01

# GraphQL
graphql.url=https://.../content-services-graphql/graphql

# TLS (set false only in dev/test)
tls.certificate.verification.enabled=false
```

### Target `FileNetConfig` (CDI bean, no file I/O)

```java
@ApplicationScoped
public class FileNetConfig {
    @Inject @ConfigProperty(name="filenet.cpe.url")    String cpeUrl;
    @Inject @ConfigProperty(name="filenet.domain")     String domain;
    @Inject @ConfigProperty(name="filenet.objectstore") String objectStore;
    // ... etc.
}
```

MicroProfile Config automatically resolves environment variable overrides
(`FILENET_CPE_URL`, `FILENET_DOMAIN`, etc.) without any custom code.

### Files changed
- `src/main/resources/META-INF/microprofile-config.properties` — consolidated, all keys added
- `src/main/resources/filenet-cli.properties` — **deleted** (superseded)
- `src/main/java/dev/fncm/service/javaapi/FileNetConfig.java` — rewritten as `@ApplicationScoped` CDI bean
- `src/main/java/dev/fncm/AppInitializer.java` — `@Produces` removed (CDI auto-manages the bean)

---

## R6 — Token TTL in `TokenCache`

**Goal:** Prevent memory growth for multi-user deployments; automatically expire sessions.

### Current problem
`TokenCache` entries live forever (until server restart or explicit logout). For a real
end-user-facing app with many concurrent users this is a memory leak and a security risk.

### Target changes

```java
// Entry gains an expiry instant
private record Entry(String username, String iamToken, Instant expiresAt) {}

// put() accepts a TTL
public void put(String zenToken, String username, String iamToken, long ttlSeconds) {
    tokenToEntry.put(zenToken,
        new Entry(username, iamToken, Instant.now().plusSeconds(ttlSeconds)));
}

// getUsername() / getIamToken() do lazy expiry on read
// A scheduled task sweeps stale entries periodically
```

The TTL value (`token.ttl.seconds`, default `3600`) is read from MicroProfile Config.

### Files changed
- `src/main/java/dev/fncm/auth/TokenCache.java` — add TTL record field, lazy expiry, sweep task
- `src/main/java/dev/fncm/resource/AuthResource.java` — pass TTL to `tokenCache.put()`
- `src/main/resources/META-INF/microprofile-config.properties` — add `token.ttl.seconds=3600`

---

## R7 — Typed JSON Responses from JACE Operations

**Goal:** Replace `StringBuilder` plain-text returns with typed Java records serialised to
JSON by Liberty JSON-B. Enables the UI to render richer output (tables, structured cards)
instead of `<pre>` blocks.

### Target return types

```java
// ConnectionTest
public record ConnectionTestResult(
    String status, String domain, String objectStore, String user) {}

// ListFolders
public record FolderItem(
    String path, String id, String created, String creator) {}

public record FolderListResult(
    int count, List<FolderItem> folders) {}
```

Liberty JSON-B serialises records automatically — no `toString()` gymnastics needed.

### Files changed
- `src/main/java/dev/fncm/model/ConnectionTestResult.java` — **new**
- `src/main/java/dev/fncm/model/FolderItem.java` — **new**
- `src/main/java/dev/fncm/model/FolderListResult.java` — **new**
- `src/main/java/dev/fncm/service/javaapi/service/ConnectionTestOperation.java` — return `ConnectionTestResult`
- `src/main/java/dev/fncm/service/javaapi/service/ListFoldersOperation.java` — return `FolderListResult`
- `src/main/webapp/js/cards/connectionTest.js` — render as table (after R2)
- `src/main/webapp/js/cards/listFolders.js` — render as table (after R2)

---

## Implementation Order

The items below are listed in the recommended sequence. Each item is self-contained
and can be implemented independently, but the listed order minimises rework.

| Step | Item | Depends on | Risk |
|------|------|------------|------|
| 1 | **R5** — Unify config | — | Low — config only, no logic change |
| 2 | **R6** — Token TTL | R5 (for config key) | Low — additive change |
| 3 | **R1** — `FileNetService` + `FileNetOperation<T>` | R5 | Medium — core refactor |
| 4 | **R7** — Typed JSON responses | R1 | Low — additive types |
| 5 | **R3** — `BaseResource` | R1 | Low — thin base class |
| 6 | **R4** — `GraphQLOperation` | R3 | Low — mirrors R1 |
| 7 | **R2** — Split JS into modules | R4, R7 | Low — UI only, no Java change |

---

## How to Trigger Implementation

When asking Bob to implement an item, reference it by ID, for example:

> "Bob, implement R1 from REFACTORING.md"
> "Bob, implement R5 and R6 from REFACTORING.md"

Bob will read this file, implement the described changes, and validate the build.
