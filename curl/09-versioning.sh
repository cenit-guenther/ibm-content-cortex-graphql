#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 09-versioning.sh checkout|checkin|cancel|versions <document-id-or-path>
action="${1:?usage: 09-versioning.sh checkout|checkin|cancel|versions <id>}"
vars="{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\",\"identifier\":\"${2:?document id}\"}"
case "$action" in
  checkout) gql 09-versioning/checkout-document.graphql "$vars" ;;
  checkin)  gql 09-versioning/checkin-document.graphql "$vars" ;;
  cancel)   gql 09-versioning/cancel-checkout.graphql "$vars" ;;
  versions) gql 09-versioning/versions.graphql "$vars" ;;
  *) echo "Unknown action: $action" >&2; exit 2 ;;
esac
