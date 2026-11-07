#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh
mkdir -p reports/logs dist
# pipefail preserves the command's real exit status.
gradle --no-daemon --max-workers=2 :core:test :core:jar | tee reports/logs/core-build.log
python3 scripts/datapack.py | tee reports/logs/datapack-validation.log
python3 scripts/test_datapack.py 2>&1 | tee reports/logs/datapack-tests.log
python3 scripts/schematics.py | tee reports/logs/schematic-validation.log
python3 scripts/test_schematics.py 2>&1 | tee reports/logs/schematic-tests.log
python3 scripts/storybook.py | tee reports/logs/storybook-validation.log
python3 scripts/test_storybook.py 2>&1 | tee reports/logs/storybook-tests.log
python3 scripts/terrain.py | tee reports/logs/terrain-validation.log
python3 scripts/test_terrain.py 2>&1 | tee reports/logs/terrain-tests.log
if [[ "${1:-full}" == full ]]; then
  extra=()
  if [[ "${PAPER_API_MODE:-published}" == source ]]; then
    bash scripts/build-source-api.sh
    extra+=("-PpaperApiClasspath=/workspace/tools/paper-api-build/build")
  fi
  if [[ -n "${LUNA_JAR:-}" ]]; then extra+=("-PlunaJar=$LUNA_JAR"); fi
  gradle --no-daemon --max-workers=2 "${extra[@]}" :plugin:clean :plugin:build :probe:jar | tee reports/logs/plugin-build.log
  cp plugin/build/libs/MidgardWinterWorld.jar dist/
  cp probe/build/libs/WinterProbe.jar dist/
fi
