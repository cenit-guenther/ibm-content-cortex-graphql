#!/usr/bin/env bash
# Shared helpers for the curl examples. Source this file; do not execute it.
#
# Required environment (see .env.example):
#   FILENET_GRAPHQL_URL, FILENET_OBJECT_STORE
#   FILENET_USER + FILENET_PASSWORD (Basic auth) or FILENET_TOKEN (Bearer auth)
# Optional: FILENET_CA_CERT (path to a PEM file with your server's CA)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Load .env from the repository root if present (without overriding existing variables).
if [[ -f "$ROOT_DIR/.env" ]]; then
  while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" == \#* ]] && continue
    value="${value%\"}"; value="${value#\"}"
    [[ -z "${!key:-}" ]] && export "$key=$value"
  done < "$ROOT_DIR/.env"
fi

require_env() {
  local missing=0
  for name in "$@"; do
    if [[ -z "${!name:-}" ]]; then
      echo "Missing environment variable: $name" >&2
      missing=1
    fi
  done
  [[ $missing -eq 0 ]] || exit 2
}

require_env FILENET_GRAPHQL_URL FILENET_OBJECT_STORE
if [[ -z "${FILENET_TOKEN:-}" ]]; then
  require_env FILENET_USER FILENET_PASSWORD
fi

# Arguments shared by every curl call: authentication and optional CA certificate.
CURL_BASE=(--silent --show-error --fail-with-body)
if [[ -n "${FILENET_TOKEN:-}" ]]; then
  CURL_BASE+=(--header "Authorization: Bearer ${FILENET_TOKEN}")
else
  CURL_BASE+=(--user "${FILENET_USER}:${FILENET_PASSWORD}")
fi
if [[ -n "${FILENET_CA_CERT:-}" ]]; then
  CURL_BASE+=(--cacert "$FILENET_CA_CERT")
fi

# Escapes a string so it can be embedded in a JSON string literal.
json_escape() {
  printf '%s' "$1" | awk 'BEGIN{ORS=""} {gsub(/\\/,"\\\\"); gsub(/"/,"\\\""); gsub(/\t/,"\\t"); gsub(/\r/,""); if (NR>1) print "\\n"; print}'
}

# Reads a .graphql file, dropping comment lines, and returns it as a JSON-escaped string.
load_query() {
  json_escape "$(grep -v '^[[:space:]]*#' "$ROOT_DIR/graphql/$1")"
}

# Pretty-prints JSON when jq is available.
# Set GQL_RAW=1 to disable pretty printing (used by test.sh to parse responses).
pretty() {
  if [[ -n "${GQL_RAW:-}" ]]; then cat; elif command -v jq >/dev/null 2>&1; then jq .; else cat; echo; fi
}

# gql <graphql file relative to graphql/> [variables JSON]
# Sends a regular JSON GraphQL request and prints the response.
gql() {
  local query variables="${2:-}"
  [[ -n "$variables" ]] || variables="{}"
  query="$(load_query "$1")"
  curl "${CURL_BASE[@]}" \
    --header "Content-Type: application/json" \
    --data "{\"query\":\"${query}\",\"variables\":${variables}}" \
    "$FILENET_GRAPHQL_URL" | pretty
}

# gql_upload <graphql file> <variables JSON> <variable name of the file content> <local file> <mime type>
# FileNet multipart upload: the variable holds the NAME of a file part ("file1"), and that part
# carries the bytes. The JSON request is sent in the "operations" part.
gql_upload() {
  local query operations
  query="$(load_query "$1")"
  operations="{\"query\":\"${query}\",\"variables\":${2}}"
  curl "${CURL_BASE[@]}" \
    --form-string "operations=${operations}" \
    --form "file1=@$4;type=$5" \
    "$FILENET_GRAPHQL_URL" | pretty
}

# json_first <key>: prints the first string value of <key> found in compact JSON on stdin.
json_first() {
  grep -o "\"$1\" *: *\"[^\"]*\"" | head -1 | sed 's/^[^:]*: *"//; s/"$//'
}
