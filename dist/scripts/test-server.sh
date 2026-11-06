#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh
# Explicit user acceptance of Minecraft EULA is required; no implied consent.
[[ "${MINECRAFT_EULA_ACCEPTED:-}" == true ]] || { echo 'Set MINECRAFT_EULA_ACCEPTED=true only after accepting https://aka.ms/MinecraftEULA'; exit 2; }
[[ $# == 1 && -f "$1" ]] || { echo 'Usage: test-server.sh /absolute/path/to/verified-paper-1.21.11.jar'; exit 2; }
server_jar=$(realpath "$1")
[[ -f dist/MidgardWinterWorld.jar && -f dist/winter-datapack.zip ]] || { echo 'Build both artifacts first'; exit 2; }
# Fixed isolated location only; never copy a live world.
mkdir -p .runtime/server/plugins .runtime/server/winter_test/datapacks
cp "$server_jar" .runtime/server/paper.jar
cp dist/MidgardWinterWorld.jar .runtime/server/plugins/
cp dist/winter-datapack.zip .runtime/server/winter_test/datapacks/
if [[ ! -e .runtime/server/server.properties ]]; then
cat > .runtime/server/server.properties <<'PROPERTIES'
level-name=winter_test
level-seed=825180311
server-ip=127.0.0.1
server-port=25575
online-mode=true
enable-rcon=false
view-distance=4
simulation-distance=3
max-players=2
PROPERTIES
fi
printf 'eula=true\n' > .runtime/server/eula.txt
cd .runtime/server
exec java "${WINTER_JAVA_FLAGS[@]}" -Xms512M -Xmx2G -jar paper.jar --nogui
