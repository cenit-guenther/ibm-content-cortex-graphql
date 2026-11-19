#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 07-download.sh <document-id-or-path> <output-file>
# Step 1 reads the download URL via GraphQL, step 2 fetches the bytes with the same credentials.
identifier="${1:?document id or path}"
output="${2:?output file}"
url="$(GQL_RAW=1 gql 07-download/document-content.graphql "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\",\"identifier\":\"$identifier\"}" | json_first downloadUrl)"
[[ -n "$url" ]] || { echo "No downloadUrl returned" >&2; exit 1; }
# FileNet returns a path relative to the context path (e.g. "/content?..."), so prepend the
# GraphQL URL without its last segment.
[[ "$url" == /* ]] && url="${FILENET_GRAPHQL_URL%/*}$url"
curl "${CURL_BASE[@]}" --output "$output" "$url"
echo "Saved $output"
