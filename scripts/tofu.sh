#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-training}"
APPLICATION_NAME="${IMAGE_NAME:-task-api}"
STATE_DIR="${TOFU_STATE_DIR:-${HOME}/.local/state/task-api-training}"
CACHE_DIR="${TOFU_CACHE_DIR:-${HOME}/.cache/task-api-training/opentofu}"
STATE_FILE="${STATE_DIR}/terraform.tfstate"

mkdir -p "${STATE_DIR}" "${CACHE_DIR}"
export TF_DATA_DIR="${CACHE_DIR}"

tofu -chdir=infrastructure init -input=false
tofu -chdir=infrastructure fmt -check
tofu -chdir=infrastructure validate

action="${1:-apply}"
case "${action}" in
  plan)
    tofu -chdir=infrastructure plan \
      -input=false \
      -state="${STATE_FILE}" \
      -var="namespace=${NAMESPACE}" \
      -var="application_name=${APPLICATION_NAME}"
    ;;
  apply)
    tofu -chdir=infrastructure plan \
      -input=false \
      -state="${STATE_FILE}" \
      -var="namespace=${NAMESPACE}" \
      -var="application_name=${APPLICATION_NAME}"
    tofu -chdir=infrastructure apply \
      -input=false \
      -auto-approve \
      -state="${STATE_FILE}" \
      -var="namespace=${NAMESPACE}" \
      -var="application_name=${APPLICATION_NAME}"
    ;;
  destroy)
    tofu -chdir=infrastructure destroy \
      -input=false \
      -auto-approve \
      -state="${STATE_FILE}" \
      -var="namespace=${NAMESPACE}" \
      -var="application_name=${APPLICATION_NAME}"
    ;;
  *)
    echo "Verwendung: $0 [plan|apply|destroy]" >&2
    exit 2
    ;;
esac
