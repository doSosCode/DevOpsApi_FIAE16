#!/usr/bin/env bash
set -euo pipefail
GRADLE_VERSION="${GRADLE_VERSION:-8.10.2}"
CACHE_DIR="${HOME}/.gradle/bootstrap"
DIST_DIR="${CACHE_DIR}/gradle-${GRADLE_VERSION}"
ZIP_FILE="${CACHE_DIR}/gradle-${GRADLE_VERSION}-bin.zip"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

if [ ! -x "${DIST_DIR}/bin/gradle" ]; then
  mkdir -p "${CACHE_DIR}"
  if [ ! -f "${ZIP_FILE}" ]; then
    echo "Gradle ${GRADLE_VERSION} wird einmalig heruntergeladen ..."
    curl -fL --retry 3       "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"       -o "${ZIP_FILE}"
  fi
  rm -rf "${DIST_DIR}"
  unzip -q "${ZIP_FILE}" -d "${CACHE_DIR}"
fi

exec "${DIST_DIR}/bin/gradle" "$@"
