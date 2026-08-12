# Configuration

This document is the single reference for all configuration in the FNCM OpenLiberty Sample: every environment variable, the MicroProfile Config property key it maps to, its default value, and important deployment notes.

---

## How Configuration Works

The application uses **MicroProfile Config** (`mpConfig-3.1`) to read configuration. Values are resolved in the following priority order (highest to lowest):

1. **Environment variables** — set in your shell, a `config.env` file, or Docker `--env-file`.
2. **`microprofile-config.properties`** — the file at `src/main/resources/META-INF/microprofile-config.properties` maps environment variable names to property keys and sets defaults.
3. **`@ConfigProperty(defaultValue = …)`** — per-field defaults in CDI beans.

MicroProfile Config automatically maps environment variables to property keys by converting `.` to `_` and upper-casing — so `FILENET_IAMHOST` is read as `iam.host` (because `microprofile-config.properties` contains `iam.host=${FILENET_IAMHOST}`).

---

## Environment Variable Reference

Set these in your `config.env` file (copy from `config.env.sample`):

```bash
cp config.env.sample config.env
# edit config.env with your values
```

### CP4BA Authentication

| Variable | Config Key | Default | Required | Description |
|---|---|---|---|---|
| `FILENET_IAMHOST` | `iam.host` | — | ✅ | IAM identity provider base URL. On CP4BA < 25.0.1 this is the `cp-console.…` hostname. On CP4BA 25.0.1+ it is the same as `FILENET_CP4BAHOST`. |
| `FILENET_CP4BAHOST` | `cp4ba.host` | — | ✅ | CP4BA platform base URL (e.g. `https://cpd-cp4ba.example.com`). Used for the Zen token exchange step. |

### Content Platform Engine (JACE)

| Variable | Config Key | Default | Required | Description |
|---|---|---|---|---|
| `FILENET_URL` | `filenet.cpe.url` | — | ✅ | Content Platform Engine WSI endpoint URL. Format: `https://{cp4ba-host}/cpe/wsi/FNCEWS40MTOM/` |
| `FILENET_DOMAIN` | `filenet.domain` | — | ✅ | FileNet P8 domain name (e.g. `P8DOMAIN`). |
| `FILENET_OBJECTSTORE` | `filenet.objectstore` | — | ✅ | Object store symbolic name (e.g. `OS1`). This value is also returned to the browser at login as `session.config.repositoryIdentifier`. |
| `FILENET_STANZA` | `filenet.stanza` | `FileNetP8WSI` | Optional | JACE login stanza name. The default `FileNetP8WSI` is correct for a standard CP4BA deployment. |

### GraphQL

| Variable | Config Key | Default | Required | Description |
|---|---|---|---|---|
| `FILENET_GRAPHQL_URL` | `graphql.url` | — | ✅ | Content Services GraphQL API URL. Format: `https://{cp4ba-host}/content-services-graphql/graphql` |

### TLS

| Variable | Config Key | Default | Required | Description |
|---|---|---|---|---|
| `TLS_CERTIFICATE_VERIFICATION_ENABLED` | `tls.certificate.verification.enabled` | `false` | Optional | Controls TLS certificate validation for all outbound HTTPS calls (to IAM, CP4BA, and GraphQL endpoints). See security note below. |

### Token Cache

| Config Key | Default | Set via | Description |
|---|---|---|---|
| `token.ttl.seconds` | `3600` | `microprofile-config.properties` | Session token lifetime in seconds. Tokens older than this value are expired automatically. Override by setting an explicit value in `microprofile-config.properties` — there is no corresponding environment variable. |

### Dev Convenience (optional)

| Variable | Config Key | Default | Description |
|---|---|---|---|
| `DEV_USER_NAME` | `dev.username` | *(empty)* | Pre-fills the login form username field. Served by the public `GET /api/config` endpoint. |
| `DEV_USER_PASSWORD` | `dev.password` | *(empty)* | Pre-fills the login form password field. |

> **Security warning**: Do not set `DEV_USER_NAME` or `DEV_USER_PASSWORD` in production. These values are returned by an unauthenticated endpoint and expose credentials in plaintext.

---

## TLS Certificate Verification

By default `TLS_CERTIFICATE_VERIFICATION_ENABLED=false`. This disables TLS certificate validation so that the application can connect to CP4BA instances that use self-signed certificates — which is typical in development and demonstration environments.

**In production or any shared environment, set `TLS_CERTIFICATE_VERIFICATION_ENABLED=true`** and ensure your CP4BA instance has a valid certificate trusted by the JVM.

When set to `false`, the `SslUtil` class installs a trust-all `TrustManager` globally on the JVM (`SSLContext.getInstance("TLS").init(…, trustAll, null)`). This affects all outbound HTTPS connections from the application.

---

## CP4BA IAM Host — Version Note

The IAM host URL depends on your CP4BA version:

| CP4BA Version | `FILENET_IAMHOST` value |
|---|---|
| Before 25.0.1 | Separate `cp-console` hostname, e.g. `https://cp-console-cp4ba.example.com` |
| 25.0.1 and later | Same as `FILENET_CP4BAHOST`, e.g. `https://cpd-cp4ba.example.com` |

If you are unsure of your CP4BA version, check your cluster configuration or CP4BA operator status. Setting the wrong IAM host is the most common cause of `HTTP 401` errors on the login step.

---

## MicroProfile Config Key Reference

Full mapping of every config key used in the application:

| Config Key | Source Env Var | Default | Injected Into |
|---|---|---|---|
| `iam.host` | `FILENET_IAMHOST` | — | `AuthService` |
| `cp4ba.host` | `FILENET_CP4BAHOST` | — | `AuthService` |
| `filenet.cpe.url` | `FILENET_URL` | — | `FileNetConfig` |
| `filenet.domain` | `FILENET_DOMAIN` | — | `FileNetConfig` |
| `filenet.objectstore` | `FILENET_OBJECTSTORE` | — | `FileNetConfig` |
| `filenet.stanza` | `FILENET_STANZA` | `FileNetP8WSI` | `FileNetConfig` |
| `graphql.url` | `FILENET_GRAPHQL_URL` | — | `GraphQLClient` |
| `tls.certificate.verification.enabled` | `TLS_CERTIFICATE_VERIFICATION_ENABLED` | `false` | `AuthService`, `GraphQLClient` |
| `token.ttl.seconds` | *(properties file only)* | `3600` | `TokenCache`, `AuthResource` |
| `dev.username` | `DEV_USER_NAME` | *(empty)* | `DevConfig` |
| `dev.password` | `DEV_USER_PASSWORD` | *(empty)* | `DevConfig` |

---

## OpenLiberty Features (server.xml)

The OpenLiberty feature set is declared in [`src/main/liberty/config/server.xml`](../src/main/liberty/config/server.xml):

| Feature | Version | Purpose |
|---|---|---|
| `restfulWS` | 3.1 | JAX-RS REST API (`@Path`, `@GET`, `@POST`, etc.) |
| `jsonb` | 3.0 | JSON binding — serializes Java records to JSON automatically |
| `mpConfig` | 3.1 | MicroProfile Config — env var → property key resolution |
| `cdi` | 4.0 | Contexts and Dependency Injection (`@Inject`, `@ApplicationScoped`, etc.) |
| `servlet` | 6.0 | Serves the SPA static files (`index.html`, `.js`, `.css`) |

**Ports**:

| Port | Protocol | Use |
|---|---|---|
| 9080 | HTTP | Default development port |
| 9443 | HTTPS | Development TLS (auto-generated self-signed certificate) |

In production, place a TLS-terminating proxy (e.g. an Ingress or API Gateway) in front of the container and run the container on HTTP port 9080 only.

---

## Configuration Template Files

| File | Purpose |
|---|---|
| [`config.env.sample`](../config.env.sample) | Template for all required environment variables |
| [`config-starter.env`](../config-starter.env) | Example with realistic placeholder values |
| [`src/main/resources/META-INF/microprofile-config.properties`](../src/main/resources/META-INF/microprofile-config.properties) | Env var → config key mapping and defaults |

---

## Related Documents

- [Authentication](authentication.md) — how `FILENET_IAMHOST` and `FILENET_CP4BAHOST` are used in the token flow
- [Backend](backend.md) — `@ConfigProperty` injection pattern
- [Architecture](architecture.md) — overall system context
