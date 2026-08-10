#!/bin/bash
# CP4BA connection settings
# Copy this file to env-vars-local.sh, fill in your values, and source it
# before running get-token.sh or call-graphql.sh:
#
#   source ./env-vars-local.sh
#
# Or export the variables in your shell before running:
#
#   export CP4BA_USER=cpmanager
#   export CP4BA_PASSWORD=mypassword
#   ...

# OpenShift namespace where CP4BA is deployed
export CP4BA_NAMESPACE="${CP4BA_NAMESPACE:-cp4ba}"

# CP4BA admin credentials
export CP4BA_USER="${CP4BA_USER:-cpmanager}"
export CP4BA_PASSWORD="${CP4BA_PASSWORD:-changeme}"

# CP4BA host URL (auto-resolved from the cluster when not set)
# To look up the host manually:
#   oc get route -n "${CP4BA_NAMESPACE}" cpd -o jsonpath="{.spec.host}"
if [ -z "${CP4BA_HOST}" ]; then
  export CP4BA_HOST="https://$(oc get route -n "${CP4BA_NAMESPACE}" cpd -o jsonpath='{.spec.host}')"
fi
