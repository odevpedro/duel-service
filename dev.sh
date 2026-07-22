#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

usage() {
  printf '%s\n' \
    "Usage: ./dev.sh <command>" \
    "" \
    "  compile      Compile Java classes only; never runs tests" \
    "  package      Build the application package with tests excluded" \
    "  native       Incrementally build and copy the configured native library" \
    "  native-full  Configure dependencies, build and copy the native library" \
    "  run          Run duel-service with the local profile" \
    "  runtime-setup Download and verify the pinned Evolution/WindBot resources" \
    "  runtime-up    Build and start only Evolution Server and WindBot" \
    "  runtime-down  Stop the local Evolution/WindBot runtime" \
    "  runtime-status Show local runtime containers" \
    "  local-play     Start the minimal runtime and the original frontend" \
    "  help         Show this help"
}

command="${1:-help}"

case "$command" in
  compile)
    exec ./gradlew localBuild
    ;;
  package)
    exec ./gradlew build -x test
    ;;
  native)
    if [[ ! -f native/ocgcore-bridge/build/CMakeCache.txt ]]; then
      printf '%s\n' "Native build is not configured. Run: ./dev.sh native-full" >&2
      exit 2
    fi
    exec ./gradlew copyNativeLib
    ;;
  native-full)
    exec ./gradlew fullBuildNative
    ;;
  run)
    exec ./gradlew bootRun --args='--spring.profiles.active=local'
    ;;
  runtime-setup)
    exec ./runtime/setup.sh
    ;;
  runtime-up)
    ./runtime/setup.sh
    exec docker compose -f runtime/docker-compose.local.yml up -d --build
    ;;
  runtime-down)
    exec docker compose -f runtime/docker-compose.local.yml down
    ;;
  runtime-status)
    exec docker compose -f runtime/docker-compose.local.yml ps
    ;;
  local-play)
    frontend="$ROOT/../yu-gi-oh-deck-management-front-end/yugioh-duel-react/yugioh-duel-react"
    if [[ ! -d "$frontend" ]]; then
      printf 'Frontend not found: %s\n' "$frontend" >&2
      exit 2
    fi
    if [[ ! -f .local-runtime/resources/BabelCDB/cards.cdb || ! -f .local-runtime/evolution-server/core/libocgcore.so ]]; then
      ./runtime/setup.sh
    fi
    docker compose -f runtime/docker-compose.local.yml up -d
    if [[ ! -x "$frontend/node_modules/.bin/vite" || ! -d "$frontend/node_modules/ygopro-msg-encode" ]]; then
      npm --prefix "$frontend" install --no-audit --no-fund
    fi
    port="${FRONTEND_PORT:-5173}"
    printf 'Local duel: http://localhost:%s/duel/local\n' "$port"
    cd "$frontend"
    exec npm run dev -- --host 0.0.0.0 --port "$port" --strictPort
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    printf 'Unknown command: %s\n\n' "$command" >&2
    usage >&2
    exit 2
    ;;
esac
