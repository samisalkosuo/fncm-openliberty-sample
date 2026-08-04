# filenet-openliberty-sample

Sample FileNet custom app using OpenLiberty.

Authentication uses CP4BA authentication.

OpenLiberty seerver has graphql proxy
and REST APIs that use Java API


## Architecture

```
Browser (index.html)
  │
  │  POST /api/auth/login  ──►  AuthResource
  │                                 └─ AuthService
  │                                      └─ OAuthClient (mpRestClient)
  │                                           └─ External IdP  ──► OAuth token
  │  ◄── { appToken, accessToken }
  │
  │  GET /api/documents    ──►  DocumentResource  (Bearer appToken)
  │                                 └─ … calls external services with accessToken
  │
  └─ GraphQL calls          ──►  External GraphQL API  (Bearer accessToken, direct)
```

| Layer | Key class / file |
|---|---|
| REST application path | `RestApplication.java` — `@ApplicationPath("/api")` |
| Login endpoint | `AuthResource` — `POST /api/auth/login` |
| OAuth IdP client | `OAuthClient` (mpRestClient) + `AuthService` |
| Secured REST resource | `DocumentResource` — `GET /api/documents` |
| Single-page UI | `src/main/webapp/index.html` |
| Server config | `src/main/liberty/config/server.xml` |
| App config | `src/main/resources/META-INF/microprofile-config.properties` |

## Token flow

1. Browser POSTs credentials to `/api/auth/login`.
2. `AuthService` forwards them to the external IdP via the `OAuthClient` REST client.
3. The IdP returns an OAuth `access_token`.
4. The server returns `{ appToken, accessToken }` to the browser.
   - **appToken** – used as `Authorization: Bearer` for every `/api/*` REST call.
   - **accessToken** – forwarded raw to the browser so the SPA can call the GraphQL
     API directly without proxying through the server.
5. mpJwt validates the Bearer token on every `/api/*` request.


## Quick start

```bash
# Dev mode (hot-reload, no Docker needed)
mvn liberty:dev

# Build WAR
mvn package -DskipTests

# Build & run container
podman build -t fncm-openliberty-sample:latest .
podman run -it --rm --name fncm-openliberty-sample -p 9080:9080 --env-file=config.env -e DEV_USER_NAME=usr -e DEV_USER_PASSWORD=pwd fncm-openliberty-sample:latest
```

Open <http://localhost:9080>.