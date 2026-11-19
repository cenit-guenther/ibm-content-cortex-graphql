#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Shows the configured object store and lists a few folders.
gql 02-repository/repository.graphql "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\"}"
gql 02-repository/repository-objects.graphql "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\"}"
