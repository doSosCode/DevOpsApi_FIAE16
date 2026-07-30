#!/usr/bin/env bash
set -euo pipefail
cat <<'EOF'
CHECKLISTE NACH DEM KOPIEREN DER VM

1. Hostname und Netzwerk prüfen:
   hostnamectl
   ip address

2. GitHub Runner nicht doppelt registriert lassen.
   Auf der Kopie alten Runner-Dienst stoppen/entfernen und neu registrieren.

3. Docker prüfen:
   docker ps

4. Minikube prüfen:
   minikube status -p minikube
   minikube start -p minikube --driver=docker   # nur falls gestoppt

5. Kubernetes-Kontext prüfen:
   kubectl config current-context
   kubectl get nodes

6. Repository außerhalb von actions-runner/_work klonen.

7. scripts/preflight.sh ausführen.
EOF
