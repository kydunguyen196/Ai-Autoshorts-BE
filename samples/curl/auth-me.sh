#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_TOKEN="${AUTH_TOKEN:-}"

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "Set AUTH_TOKEN before calling protected endpoints."
  exit 1
fi

curl -sS "${BASE_URL}/api/auth/me" \
  -H "Authorization: Bearer ${AUTH_TOKEN}"
echo
