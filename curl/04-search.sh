#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 04-search.sh [where-clause] [class]
# Example: 04-search.sh "DocumentTitle LIKE 'Invoice%'"
where="${1:-}"
class="${2:-Document}"
where_json="null"
[[ -n "$where" ]] && where_json="\"$(json_escape "$where")\""
gql 04-search/search-documents.graphql "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\",\"from\":\"$class\",\"where\":$where_json,\"pageSize\":10}"
