#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-training}"
SECRET_NAME="task-api-database"

if kubectl get secret "${SECRET_NAME}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
  echo "Secret ${SECRET_NAME} existiert bereits und bleibt unverändert."
  exit 0
fi

DB_NAME="${DB_NAME:-taskdb}"
DB_USER="${DB_USER:-taskapp}"
# Hex-Zeichen vermeiden Shell-, YAML- und URL-Sonderfälle bei der Demonstration.
DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -hex 32)}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-$(openssl rand -hex 40)}"

kubectl create secret generic "${SECRET_NAME}" \
  --namespace "${NAMESPACE}" \
  --from-literal=database-name="${DB_NAME}" \
  --from-literal=database-user="${DB_USER}" \
  --from-literal=database-password="${DB_PASSWORD}" \
  --from-literal=database-root-password="${DB_ROOT_PASSWORD}"

echo "Secret ${SECRET_NAME} wurde erstellt. Kennwörter werden nicht ausgegeben."
