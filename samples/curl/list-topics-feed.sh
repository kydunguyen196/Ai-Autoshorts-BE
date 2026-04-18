#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
PAGE="${PAGE:-0}"
LIMIT="${LIMIT:-20}"
STATUS="${STATUS:-}"
if [[ -z "${AUTH_TOKEN:-}" ]]; then
  echo "AUTH_TOKEN is required"
  exit 1
fi
URL="${BASE_URL}/api/topics/feed?page=${PAGE}&limit=${LIMIT}"
if [[ -n "${STATUS}" ]]; then
  URL="${URL}&status=${STATUS}"
fi
curl -sS "$URL" -H "Authorization: Bearer ${AUTH_TOKEN}"
