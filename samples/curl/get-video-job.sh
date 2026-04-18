#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <jobId>"
  exit 1
fi

JOB_ID="$1"
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_TOKEN="${AUTH_TOKEN:-}"

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "Set AUTH_TOKEN before calling protected endpoints."
  exit 1
fi

curl -sS "${BASE_URL}/api/videos/${JOB_ID}" \
  -H "Authorization: Bearer ${AUTH_TOKEN}"
echo
