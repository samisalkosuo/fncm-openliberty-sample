#!/bin/bash
# get-token.sh — Obtain IAM and Zen tokens from an IBM CP4BA server.
# Requires CP4BA 25.0.1 or later.
#
# Usage:
#   source ./env-vars.sh   # or set CP4BA_USER / CP4BA_PASSWORD / CP4BA_NAMESPACE / CP4BA_HOST
#   ./get-token.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/env-vars.sh"

echo "CP4BA host : ${CP4BA_HOST}"
echo "User       : ${CP4BA_USER}"

# Step 1 — IAM token
iam_token=$(curl -sk -X POST \
  -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" \
  -d "grant_type=password&username=${CP4BA_USER}&password=${CP4BA_PASSWORD}&scope=openid" \
  "${CP4BA_HOST}/idprovider/v1/auth/identitytoken" \
  | python -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

echo "iam_token  : ${iam_token}"

# Step 2 — Zen token
zen_token=$(curl -sk \
  -H "username: ${CP4BA_USER}" \
  -H "iam-token: ${iam_token}" \
  "${CP4BA_HOST}/v1/preauth/validateAuth" \
  | python -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')

echo "zen_token  : ${zen_token}"
