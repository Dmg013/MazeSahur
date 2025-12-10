#!/usr/bin/env bash
set -euo pipefail

# Build the server fat JAR so Docker has the artifact to copy.
./gradlew :server:jar

# Ensure a buildx builder exists (ignore if it already does).
docker buildx create --use --name mazesahurbuilder 2>/dev/null || true

# Build and push a multi-arch image for the server.
docker buildx build --platform linux/amd64 \
  -t sympactdev/mazesahur-server:latest \
  -f server/Dockerfile server \
  --push
