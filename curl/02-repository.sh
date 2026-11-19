#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Lists all repositories, then shows the configured object store.
gql 02-repository/repositories.graphql
gql 02-repository/repository.graphql "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\"}"
