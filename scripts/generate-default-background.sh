#!/usr/bin/env bash
set -euo pipefail

OUTPUT="${1:-assets/backgrounds/default.mp4}"
DURATION="${2:-60}"

ffmpeg -y -f lavfi -i "color=c=0x202020:s=1080x1920:r=30" -t "${DURATION}" -c:v libx264 -pix_fmt yuv420p "${OUTPUT}"
