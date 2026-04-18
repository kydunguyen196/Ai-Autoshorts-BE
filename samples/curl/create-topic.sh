#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "Set AUTH_TOKEN before calling protected endpoints."
  exit 1
fi

curl -sS -X POST "${BASE_URL}/api/topics" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  -d @"${PROJECT_ROOT}/samples/requests/topic-create.json"

echo
