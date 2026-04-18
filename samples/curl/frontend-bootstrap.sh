#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
if [[ -z "${AUTH_TOKEN:-}" ]]; then
  echo "AUTH_TOKEN is required"
  exit 1
fi
curl -sS "${BASE_URL}/api/frontend/bootstrap" -H "Authorization: Bearer ${AUTH_TOKEN}"
