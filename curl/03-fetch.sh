#!/usr/bin/env bash
source "$(dirname "$0")/lib.sh"

# Usage: 03-fetch.sh document|folder <path-or-id>
# Example: 03-fetch.sh folder /
kind="${1:?usage: 03-fetch.sh document|folder <path-or-id>}"
identifier="${2:?usage: 03-fetch.sh document|folder <path-or-id>}"
gql "03-fetch/$kind.graphql" "{\"repositoryIdentifier\":\"$FILENET_OBJECT_STORE\",\"identifier\":\"$identifier\"}"
