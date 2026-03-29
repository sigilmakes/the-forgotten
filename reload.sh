#!/usr/bin/env bash
# Compile and hot-swap changed classes into running Minecraft
set -e

echo "Compiling..."
./gradlew classes -q

echo "Hot-swapping..."
java hotswap.java build/classes/java/main --changed-since 10
