#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-training}"
DELETE_DATA="${DELETE_DATA:-false}"

if [[ "${DELETE_DATA}" == "true" ]]; then
  echo "Namespace inklusive MariaDB-PVC und Daten wird gelöscht."
  K8S_NAMESPACE="${NAMESPACE}" ./scripts/tofu.sh destroy
else
  echo "API-Ressourcen werden entfernt; MariaDB und Daten bleiben bestehen."
  kubectl delete \
    deployment/task-api \
    service/task-api \
    ingress/task-api \
    secret/task-api-tls \
    poddisruptionbudget/task-api \
    configmap/task-api-config \
    --namespace "${NAMESPACE}" \
    --ignore-not-found=true
fi

echo "Minikube und lokale CA wurden nicht gelöscht."
