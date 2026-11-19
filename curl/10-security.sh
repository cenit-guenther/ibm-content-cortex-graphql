#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 10-security.sh <document-id-or-path>
gql 10-security/permissions.graphql "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\",\"identifier\":\"${1:?document id}\"}"
