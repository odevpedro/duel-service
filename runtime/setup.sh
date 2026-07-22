#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.local-runtime"

# shellcheck source=versions.env
source "$ROOT_DIR/runtime/versions.env"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 1
  fi
}

checkout_commit() {
  local url="$1"
  local commit="$2"
  local destination="$3"

  if [[ ! -d "$destination/.git" ]]; then
    mkdir -p "$(dirname "$destination")"
    git clone --no-checkout --filter=blob:none "$url" "$destination"
  fi

  git -C "$destination" fetch --depth 1 origin "$commit"
  git -C "$destination" checkout --detach "$commit"

  local actual
  actual="$(git -C "$destination" rev-parse HEAD)"
  if [[ "$actual" != "$commit" ]]; then
    echo "revision mismatch for $destination: expected $commit, got $actual" >&2
    exit 1
  fi
}

for command in git curl unzip sha256sum; do
  require_command "$command"
done

mkdir -p "$RUNTIME_DIR/downloads" "$RUNTIME_DIR/resources" "$RUNTIME_DIR/windbot-release"

checkout_commit \
  "https://github.com/diangogav/EDOpro-server-ts.git" \
  "$EVOLUTION_SERVER_COMMIT" \
  "$RUNTIME_DIR/evolution-server"

checkout_commit \
  "https://github.com/ProjectIgnis/CardScripts.git" \
  "$CARDSCRIPTS_COMMIT" \
  "$RUNTIME_DIR/resources/CardScripts"

checkout_commit \
  "https://github.com/ProjectIgnis/BabelCDB.git" \
  "$BABELCDB_COMMIT" \
  "$RUNTIME_DIR/resources/BabelCDB"

checkout_commit \
  "https://github.com/ProjectIgnis/LFLists.git" \
  "$LFLISTS_COMMIT" \
  "$RUNTIME_DIR/resources/LFLists"

archive="$RUNTIME_DIR/downloads/$WINDBOT_ARCHIVE"
if [[ ! -f "$archive" ]]; then
  curl -fL \
    "https://github.com/ProjectIgnis/windbot/releases/download/$WINDBOT_TAG/$WINDBOT_ARCHIVE" \
    -o "$archive"
fi

printf '%s  %s\n' "$WINDBOT_SHA256" "$archive" | sha256sum --check --status
unzip -q -o "$archive" -d "$RUNTIME_DIR/windbot-release"

core="$RUNTIME_DIR/evolution-server/core/libocgcore.so"
printf '%s  %s\n' "$EVOLUTION_OCGCORE_SHA256" "$core" | sha256sum --check --status

deck="$ROOT_DIR/runtime/decks/AI_BE2025.ydk"
database="$RUNTIME_DIR/resources/BabelCDB/cards.cdb"
scripts="$RUNTIME_DIR/resources/CardScripts"

for resource in "$deck" "$database" "$scripts/constant.lua" "$scripts/utility.lua"; do
  if [[ ! -f "$resource" ]]; then
    echo "required runtime resource is missing: $resource" >&2
    exit 1
  fi
done

image_cache="$RUNTIME_DIR/card-images"
mkdir -p "$image_cache"
while read -r code; do
  image="$image_cache/$code.jpg"
  if [[ ! -s "$image" ]]; then
    curl -fsSL --retry 2 "https://images.ygoprodeck.com/images/cards/$code.jpg" -o "$image"
  fi
done < <(awk '/^[0-9]+$/{print}' "$deck" | sort -u)

main_count="$(awk '/^#main/{inside=1;next}/^#extra/{inside=0} inside && /^[0-9]+$/{count++} END{print count+0}' "$deck")"
if [[ "$main_count" -ne 40 ]]; then
  echo "WindBot Blue-Eyes main deck must contain 40 cards, found $main_count" >&2
  exit 1
fi

echo "local duel resources are ready"
echo "  evolution-server: $EVOLUTION_SERVER_COMMIT"
echo "  CardScripts:      $CARDSCRIPTS_COMMIT"
echo "  BabelCDB:         $BABELCDB_COMMIT"
echo "  WindBot:          $WINDBOT_COMMIT"
echo "  Blue-Eyes 2025:   $main_count legal main-deck cards"
echo "  Card images:      cached locally"
