#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Uso: ./scripts/publish-working-tree.sh <version-unica>" >&2
    exit 2
fi

if [[ -z "${GITHUB_ACTOR:-}" || -z "${GITHUB_TOKEN:-}" ]]; then
    echo "Faltan GITHUB_ACTOR/GITHUB_TOKEN (o gpr.user/gpr.key en ~/.gradle/gradle.properties)." >&2
    exit 2
fi

readonly VERSION="$1"

# Publica exactamente los fuentes presentes, incluso cambios todavía no commiteados,
# pero solo después de que pasen calidad, tests internos y el TCK oficial.
./gradlew clean build jacocoRootReport -PprintscriptVersion="$VERSION" \
    --no-daemon --warning-mode all
./tck/run-official-tck.sh "$VERSION"
./gradlew publish -PprintscriptVersion="$VERSION" --no-daemon
