#!/usr/bin/env bash
set -euo pipefail

# Minimaler Bootstrap: Nur Ansible wird per Shell installiert. Alle weiteren
# Pakete und Konfigurationen verwaltet anschließend Ansible idempotent.
if ! sudo -n true 2>/dev/null; then
  echo "FEHLER: Der Runner-Benutzer benötigt passwortloses sudo."
  echo "Siehe docs/DOZENTEN_ANLEITUNG.md."
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
sudo apt-get update
sudo apt-get install -y ansible-core python3 python3-apt
ansible-playbook --version
