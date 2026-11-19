#!/usr/bin/env bash
# Runs the integration tests of all implementations. Each suite skips itself when the
# FILENET_* environment (or .env) is not configured.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "=== curl ===";    bash curl/test.sh
echo "=== Node.js ==="; (cd nodejs && npm test)
echo "=== Java ===";    (cd java && mvn -q -B test)
