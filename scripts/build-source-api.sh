#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh
mkdir -p reports/logs
# Compile official unmodified Paper/Brigadier sources. This is NOT a server or an official API release binary.
python3 scripts/source_api.py
cp scripts/support/paper-api.gradle.kts /workspace/tools/paper-api-build/build.gradle.kts
printf 'rootProject.name = "paper-api-source-build"\n' > /workspace/tools/paper-api-build/settings.gradle.kts
gradle --no-daemon --max-workers=2 -p /workspace/tools/paper-api-build jar compileDependencies | tee reports/logs/paper-api-source-build.log
