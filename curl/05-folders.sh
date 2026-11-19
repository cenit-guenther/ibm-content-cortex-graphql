#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 05-folders.sh create <name> [parent=/] | rename <id-or-path> <new-name> | delete <id-or-path>
action="${1:?usage: 05-folders.sh create|rename|delete ...}"
os="$FILENET_OBJECT_STORE"
case "$action" in
  create) gql 05-folders/create-folder.graphql "{\"repositoryIdentifier\":\"$os\",\"name\":\"${2:?name}\",\"parent\":\"${3:-/}\"}" ;;
  rename) gql 05-folders/update-folder.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"${2:?id}\",\"name\":\"${3:?new name}\"}" ;;
  delete) gql 05-folders/delete-folder.graphql "{\"repositoryIdentifier\":\"$os\",\"identifier\":\"${2:?id}\"}" ;;
  *) echo "Unknown action: $action" >&2; exit 2 ;;
esac
