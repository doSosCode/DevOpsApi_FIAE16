#!/usr/bin/env bash
set -euo pipefail

PROFILE="${MINIKUBE_PROFILE:-minikube}"
TLS_HOSTNAME="${TLS_HOSTNAME:-task-api.local}"
TLS_DIR="${TLS_DIR:-${HOME}/.local/share/task-api-training/tls}"
CA_CERT="${TLS_DIR}/training-ca.crt"
MINIKUBE_IP="$(minikube ip --profile "${PROFILE}")"

if [[ ! -f "${CA_CERT}" ]]; then
  echo "FEHLER: CA-Zertifikat fehlt: ${CA_CERT}"
  exit 1
fi

# --resolve prüft Hostname, SNI und Zertifikat ohne /etc/hosts zu ändern.
curl --fail --silent --show-error \
  --retry 18 --retry-delay 5 --retry-all-errors \
  --cacert "${CA_CERT}" \
  --resolve "${TLS_HOSTNAME}:443:${MINIKUBE_IP}" \
  "https://${TLS_HOSTNAME}/api/health"

echo
echo "HTTPS-Smoke-Test erfolgreich."
