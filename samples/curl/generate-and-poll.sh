#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
REQUEST_FILE="${REQUEST_FILE:-samples/requests/generate-video.json}"
AUTH_TOKEN="${AUTH_TOKEN:-}"

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "Set AUTH_TOKEN before calling protected endpoints."
  exit 1
fi

echo "Submitting generation job..."
CREATE_RESPONSE=$(curl -sS -X POST "${API_BASE}/api/videos/generate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  --data @"${REQUEST_FILE}")

echo "${CREATE_RESPONSE}"
JOB_ID=$(echo "${CREATE_RESPONSE}" | sed -n 's/.*"jobId":"\([^"]*\)".*/\1/p')

if [[ -z "${JOB_ID}" ]]; then
  echo "Failed to parse jobId from response"
  exit 1
fi

echo "Polling job ${JOB_ID}..."
for i in {1..30}; do
  STATUS_RESPONSE=$(curl -sS "${API_BASE}/api/videos/${JOB_ID}" \
    -H "Authorization: Bearer ${AUTH_TOKEN}")
  STATUS=$(echo "${STATUS_RESPONSE}" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
  echo "Attempt ${i} => ${STATUS}"
  echo "${STATUS_RESPONSE}"
  if [[ "${STATUS}" == "COMPLETED" || "${STATUS}" == "FAILED" ]]; then
    break
  fi
  sleep 2
done
