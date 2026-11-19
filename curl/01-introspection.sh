#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 01-introspection.sh [TypeName]
# Without an argument: schema overview. With a type name (e.g. Document): its fields.
if [[ $# -ge 1 ]]; then
  gql 01-introspection/type-details.graphql "{\"name\":\"$1\"}"
else
  gql 01-introspection/schema-overview.graphql
fi
