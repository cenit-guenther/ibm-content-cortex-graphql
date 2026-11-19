#!/usr/bin/env bash
# Integration test: runs a full lifecycle against a real FileNet server.
# Skips (exit 0) when the required environment is not configured.
# Creates a uniquely named folder under FILENET_TEST_FOLDER (default "/") and removes it at the end.
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -f ../.env ]] && [[ -z "${FILENET_GRAPHQL_URL:-}" ]]; then
  echo "SKIP: FILENET_GRAPHQL_URL not set"; exit 0
fi

source ./lib.sh
export GQL_RAW=1
os="$FILENET_OBJECT_STORE"
parent="${FILENET_TEST_FOLDER:-/}"
suffix="$(date +%s)-$$"
folder_name="gql-example-$suffix"
tmp="$(mktemp -d)"
folder_id=""
doc_id=""

cleanup() {
  rm -rf "$tmp"
  if [[ -n "$doc_id" ]]; then
    # Delete every version of the document (errors are ignored during cleanup).
    local ids
    ids="$(gql 09-versioning/versions.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$doc_id\"}" 2>/dev/null | grep -o '{[0-9A-Fa-f-]\{36\}}' | sort -u || true)"
    for id in $ids; do
      [[ "$id" == "$doc_id" ]] && continue
      gql 06-upload/delete-document.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$id\"}" >/dev/null 2>&1 || true
    done
    gql 06-upload/delete-document.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$doc_id\"}" >/dev/null 2>&1 || true
  fi
  if [[ -n "$folder_id" ]]; then
    gql 05-folders/delete-folder.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$folder_id\"}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1" >&2; exit 1; }
no_errors() { [[ "$1" != *'"errors"'* ]] || fail "$2: $1"; }

out="$(gql 01-introspection/schema-overview.graphql)"
no_errors "$out" introspection; [[ "$out" == *queryType* ]] || fail "introspection"; pass introspection

out="$(gql 02-repository/repository.graphql "{\"repositoryIdentifier\":\"$os\"}")"
no_errors "$out" repository; pass repository

out="$(gql 05-folders/create-folder.graphql "{\"repositoryIdentifier\":\"$os\",\"name\":\"$folder_name\",\"parent\":\"$parent\"}")"
no_errors "$out" create-folder
folder_id="$(printf '%s' "$out" | json_first id)"
[[ -n "$folder_id" ]] || fail "create-folder returned no id"; pass "create folder $folder_id"

echo "hello FileNet GraphQL $suffix" > "$tmp/hello.txt"
out="$(gql_upload 06-upload/create-document.graphql "{\"repositoryIdentifier\":\"$os\",\"name\":\"hello-$suffix.txt\",\"folder\":\"$folder_id\",\"contentType\":\"text/plain\",\"file\":\"file1\"}" file "$tmp/hello.txt" text/plain)"
no_errors "$out" upload
doc_id="$(printf '%s' "$out" | json_first id)"
[[ -n "$doc_id" ]] || fail "upload returned no id"; pass "upload document $doc_id"

out="$(gql 03-fetch/document.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$doc_id\"}")"
no_errors "$out" fetch-document; pass "fetch document by id"

out="$(gql 03-fetch/folder.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$folder_id\"}")"
no_errors "$out" fetch-folder; [[ "$out" == *"hello-$suffix.txt"* ]] || fail "folder does not list the document"; pass "fetch folder"

out="$(gql 04-search/search-documents.graphql "{\"repositoryIdentifier\":\"$os\",\"from\":\"Document\",\"where\":\"This INFOLDER '$folder_id'\",\"pageSize\":10}")"
no_errors "$out" search; [[ "$out" == *"hello-$suffix.txt"* ]] || fail "search did not find the document"; pass search

./07-download.sh "$doc_id" "$tmp/downloaded.txt" >/dev/null
cmp "$tmp/hello.txt" "$tmp/downloaded.txt" || fail "downloaded content differs"; pass download

out="$(gql 08-properties/update-document.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$doc_id\",\"name\":\"renamed-$suffix.txt\",\"title\":\"Title $suffix\"}")"
no_errors "$out" update-document; [[ "$out" == *"renamed-$suffix.txt"* ]] || fail "rename failed"; pass "update properties"

out="$(gql 10-security/permissions.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$doc_id\"}")"
no_errors "$out" permissions; pass permissions

out="$(gql 09-versioning/checkout-document.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$doc_id\"}")"
no_errors "$out" checkout
res_id="$(printf '%s' "$out" | grep -o '"reservation":{"id":"[^"]*"' | sed 's/.*:"//; s/"$//')"
[[ -n "$res_id" ]] || fail "checkout returned no reservation id"
out="$(gql 09-versioning/checkin-document.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"$res_id\"}")"
no_errors "$out" checkin; pass "checkout / checkin"

echo "All curl integration tests passed."
