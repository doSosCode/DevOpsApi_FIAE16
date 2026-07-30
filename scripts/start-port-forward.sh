#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-training}"
BIND_ADDRESS="${BIND_ADDRESS:-0.0.0.0}"
HOST_PORT="${HOST_PORT:-8081}"

cat <<INFO
Temporäres Port-Forwarding:
  VM-Port ${HOST_PORT} -> Kubernetes-Service task-api:8080

Der Prozess läuft nur, solange dieses Terminal geöffnet bleibt.
INFO

exec kubectl port-forward \
  --namespace "${NAMESPACE}" \
  --address "${BIND_ADDRESS}" \
  service/task-api \
  "${HOST_PORT}:8080"
