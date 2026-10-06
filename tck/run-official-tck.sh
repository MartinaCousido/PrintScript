#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 || ( $# -eq 2 && "$2" != --published ) ]]; then
    echo "Uso: ./tck/run-official-tck.sh <version> [--published]" >&2
    exit 2
fi

readonly VERSION="$1"
readonly SOURCE_MODE="${2:-local}"
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[[:alnum:].]+)?$ ]]; then
    echo "Versión inválida: $VERSION" >&2
    exit 2
fi
if [[ "$SOURCE_MODE" == --published ]]; then
    : "${GITHUB_ACTOR:?Falta GITHUB_ACTOR}"
    : "${GITHUB_TOKEN:?Falta GITHUB_TOKEN}"
fi
readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly IMPLEMENTATION_DIR="${PRINTSCRIPT_TCK_IMPLEMENTATION_SOURCE:-$ROOT_DIR/tck/implementation}"
readonly TCK_DIR="$ROOT_DIR/build/official-tck"
readonly TCK_COMMIT="510028a8246f6fd607a3195394310f509d9047a2"

cd "$ROOT_DIR"

# Maven Local es el puente deliberado entre ambos builds: el TCK prueba estos jars,
# publicados desde el working tree, no una versión vieja descargada de Internet.
if [[ "$SOURCE_MODE" == local ]]; then
    ./gradlew publishToMavenLocal -PprintscriptVersion="$VERSION" --no-daemon
fi

rm -rf "$TCK_DIR"
git clone --quiet https://github.com/austral-ingsis/printscript-tck.git "$TCK_DIR"
git -C "$TCK_DIR" checkout --quiet "$TCK_COMMIT"

cp "$IMPLEMENTATION_DIR/CustomImplementationFactory.java" \
    "$TCK_DIR/src/main/java/implementation/CustomImplementationFactory.java"
cp "$IMPLEMENTATION_DIR/PrintScriptAdapters.java" \
    "$TCK_DIR/src/main/java/implementation/PrintScriptAdapters.java"
rm "$TCK_DIR/src/main/java/implementation/NotImplementedException.java"

PRINTSCRIPT_TCK_SOURCE_MODE="$SOURCE_MODE" PRINTSCRIPT_TCK_VERSION="$VERSION" ./gradlew \
    -p "$TCK_DIR" \
    --init-script "$ROOT_DIR/tck/tck.init.gradle" \
    clean test --no-daemon --console=plain
