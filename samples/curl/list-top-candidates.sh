#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
GROUP_ID="${GROUP_ID:-}"
PAGE="${PAGE:-0}"
LIMIT="${LIMIT:-10}"
if [[ -z "${AUTH_TOKEN:-}" ]]; then
  echo "AUTH_TOKEN is required"
  exit 1
fi
if [[ -z "${GROUP_ID}" ]]; then
  echo "GROUP_ID is required"
  exit 1
fi
URL="${BASE_URL}/api/videos/group/${GROUP_ID}/top-candidates?page=${PAGE}&limit=${LIMIT}"
curl -sS "$URL" -H "Authorization: Bearer ${AUTH_TOKEN}"
