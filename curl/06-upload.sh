#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 06-upload.sh <local-file> <folder-id-or-path> [document-name] [mime-type]
file="${1:?local file}"
folder="${2:?target folder}"
name="${3:-$(basename "$file")}"
mime="${4:-text/plain}"
gql_upload 06-upload/create-document.graphql \
  "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\",\"name\":\"$name\",\"folder\":\"$folder\",\"contentType\":\"$mime\",\"file\":null}" \
  file "$file" "$mime"
