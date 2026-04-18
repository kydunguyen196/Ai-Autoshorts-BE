#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
LIMIT="${LIMIT:-20}"
STATUS="${STATUS:-}"
AUTH_TOKEN="${AUTH_TOKEN:-}"

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "Set AUTH_TOKEN before calling protected endpoints."
  exit 1
fi

URL="${BASE_URL}/api/videos?limit=${LIMIT}"
if [[ -n "${STATUS}" ]]; then
  URL="${URL}&status=${STATUS}"
fi

curl -sS "${URL}" \
  -H "Authorization: Bearer ${AUTH_TOKEN}"
echo
