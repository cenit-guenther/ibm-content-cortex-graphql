#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 08-properties.sh <document-id-or-path> <new-name> <title>
gql 08-properties/update-document.graphql "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\",\"identifier\":\"${1:?id}\",\"name\":\"${2:?name}\",\"title\":\"${3:?title}\"}"
