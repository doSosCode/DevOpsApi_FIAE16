#!/usr/bin/env bash
set -euo pipefail

TLS_DIR="${TLS_DIR:-${HOME}/.local/share/task-api-training/tls}"
SOURCE="${TLS_DIR}/training-ca.crt"
TARGET="${1:-./task-api-training-ca.crt}"

if [[ ! -f "${SOURCE}" ]]; then
  echo "FEHLER: CA-Zertifikat fehlt. Führe zuerst die Deployment-Pipeline aus."
  exit 1
fi

install -m 0644 "${SOURCE}" "${TARGET}"
echo "CA-Zertifikat exportiert nach: ${TARGET}"
