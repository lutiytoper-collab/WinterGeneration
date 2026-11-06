#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .runtime/client
for name in package.json package-lock.json; do
  if [[ -f ".runtime/client/$name" ]] && ! cmp -s "qa-client/$name" ".runtime/client/$name"; then
    echo "Preserving a different .runtime/client/$name; archive or review it first" >&2
    exit 2
  fi
  cp "qa-client/$name" ".runtime/client/$name"
done
npm ci --prefix .runtime/client --no-audit --no-fund
