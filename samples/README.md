# CP4BA GraphQL API — Shell Script Examples

> **Requirement:** IBM Cloud Pak for Business Automation (CP4BA) **25.0.1 or later**.

These scripts demonstrate how to authenticate against a CP4BA cluster and call the
Content Services GraphQL API using plain `curl` and `python`.

---

## Files

| File | Description |
|------|-------------|
| `env-vars.sh` | Shared environment variables sourced by the other scripts |
| `get-token.sh` | Obtain an IAM token and a Zen token |
| `call-graphql.sh` | Obtain a Zen token and call the GraphQL endpoint |

---

## Prerequisites

| Tool | Purpose |
|------|---------|
| `oc` | OpenShift CLI — used to resolve the CP4BA host route |
| `curl` | HTTP client |
| `python` | JSON parsing (standard library only) |

You must be logged in to the OpenShift cluster with `oc login` before running the scripts, or you can set `CP4BA_HOST` manually (see below).

---

## Configuration

The scripts share their settings through four environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `CP4BA_NAMESPACE` | `cp4ba` | OpenShift namespace where CP4BA is deployed |
| `CP4BA_USER` | `cpmanager` | CP4BA username |
| `CP4BA_PASSWORD` | `changeme` | CP4BA password |
| `CP4BA_HOST` | *(auto-resolved)* | Base URL of the CP4BA host, e.g. `https://cpd-cp4ba.apps.example.com` |

### Option A — export variables in your shell

```bash
export CP4BA_NAMESPACE=cp4ba
export CP4BA_USER=cpmanager
export CP4BA_PASSWORD=mysecretpassword
# CP4BA_HOST is resolved automatically via oc if not set
```

### Option B — create a local override file

Copy `env-vars.sh` to `env-vars-local.sh` (never commit this file), fill in your
values, and source it before running a script:

```bash
cp env-vars.sh env-vars-local.sh
# edit env-vars-local.sh with your credentials
source ./env-vars-local.sh
```

> **Tip:** Add `env-vars-local.sh` to `.gitignore` to avoid accidentally committing
> credentials.

---

## Look up the CP4BA host with `oc`

If you want to find the host URL manually without running the scripts:

```bash
# Make sure you are logged in first:
oc login --token=<token> --server=https://<api-server>:6443

# Then retrieve the CPD route host:
oc get route -n cp4ba cpd -o jsonpath="{.spec.host}"
# Example output: cpd-cp4ba.apps.cluster.example.com

# Construct the full URL:
echo "https://$(oc get route -n cp4ba cpd -o jsonpath='{.spec.host}')"
```

---

## Usage

### Make scripts executable (first time only)

```bash
chmod +x env-vars.sh get-token.sh call-graphql.sh
```

### Get an IAM token and Zen token

```bash
./get-token.sh
```

Example output:

```
CP4BA host : https://cpd-cp4ba.apps.cluster.example.com
User       : cpmanager
iam_token  : eyJhbGciOiJS...
zen_token  : eyJhbGciOiJS...
```

### Call the GraphQL API

```bash
./call-graphql.sh
```

The sample query lists all object store symbolic names:

```graphql
{ domain { objectStores { objectStores { symbolicName } } } }
```

---

## Authentication flow

```
┌──────────────────────────────────────────────────────────┐
│  1. POST /idprovider/v1/auth/identitytoken               │
│     username + password  →  iam_token                    │
│                                                          │
│  2. GET  /v1/preauth/validateAuth                        │
│     iam_token            →  zen_token                    │
│                                                          │
│  3. POST /content-services-graphql/graphql               │
│     zen_token + XSRF     →  GraphQL response             │
└──────────────────────────────────────────────────────────┘
```

---

## Notes

- The XSRF token value used in `call-graphql.sh` is a static example UUID. For
  production use, retrieve it dynamically from the CP4BA session.
- The scripts use `python` (v2 or v3) only for JSON parsing. No third-party
  packages are required.
- Tested against **CP4BA 25.0.1**. Earlier versions are not supported.
