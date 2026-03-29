#!/usr/bin/env bash
# Compile and hot-swap changed classes into running Minecraft
set -e

cd "$(dirname "$0")"

echo "Compiling..."
nix develop --command bash -c '
    ./gradlew classes -q
    echo "Hot-swapping..."
    javac -d build/hotswap hotswap.java 2>/dev/null
    java -cp build/hotswap hotswap build/classes/java/main --changed-since 10
'
