#!/usr/bin/env bash
set -euo pipefail

# Zentraler Vorabcheck für lokale Deployments. Ansible prüft die Werkzeuge und
# startet Minikube bei Bedarf; deshalb wird Minikube hier nicht vorzeitig geprüft.
PROFILE="${MINIKUBE_PROFILE:-minikube}"
INVENTORY="${ANSIBLE_INVENTORY:-ansible/inventory.ini}"
PLAYBOOK="${ANSIBLE_PLAYBOOK:-ansible/playbook.yml}"

required_commands=(git java docker kubectl minikube tofu curl openssl jq ansible-playbook)

for command_name in "${required_commands[@]}"; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "FEHLT: ${command_name}"
    echo "Starte zuerst den Workflow '01 - Bootstrap Ubuntu VM'."
    exit 1
  fi
done

[[ -f "${INVENTORY}" ]] || { echo "FEHLT: ${INVENTORY}"; exit 1; }
[[ -f "${PLAYBOOK}" ]] || { echo "FEHLT: ${PLAYBOOK}"; exit 1; }

docker info >/dev/null

ansible-playbook --inventory "${INVENTORY}" "${PLAYBOOK}" --syntax-check
ansible-playbook \
  --inventory "${INVENTORY}" \
  "${PLAYBOOK}" \
  --extra-vars "minikube_profile=${PROFILE}"

echo "Umgebung ist einsatzbereit."
