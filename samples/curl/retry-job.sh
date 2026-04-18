#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <jobId>"
  exit 1
fi

API_BASE="${API_BASE:-http://localhost:8080}"
JOB_ID="$1"
AUTH_TOKEN="${AUTH_TOKEN:-}"

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "Set AUTH_TOKEN before calling protected endpoints."
  exit 1
fi

curl -sS -X POST "${API_BASE}/api/videos/${JOB_ID}/retry" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${AUTH_TOKEN}"
