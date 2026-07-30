#!/usr/bin/env bash
set -euo pipefail

# Standardmäßig wird die lokale Docker-Compose-API verwendet.
# Für Minikube kann BASE_URL von außen gesetzt werden:
# BASE_URL="http://$(minikube ip -p minikube):30081" ./scripts/seed-demo-data.sh
BASE_URL="${BASE_URL:-http://localhost:8080}"

create_task() {
  local title="$1"
  local description="$2"
  local priority="$3"
  local due_date="$4"

  curl --fail --silent --show-error \
    --request POST \
    --url "${BASE_URL}/api/tasks" \
    --header 'Content-Type: application/json' \
    --data "$(jq -n \
      --arg title "${title}" \
      --arg description "${description}" \
      --arg priority "${priority}" \
      --arg dueDate "${due_date}" \
      '{
        title: $title,
        description: $description,
        priority: $priority,
        dueDate: $dueDate
      }')"
}

echo "Erzeuge Demo-Daten über ${BASE_URL} ..."

create_task \
  "CI/CD-Pipeline erklären" \
  "GitHub Actions, Runner und Deployment im Unterricht zeigen" \
  "HIGH" \
  "2030-06-30"

echo

create_task \
  "MariaDB-Persistenz demonstrieren" \
  "Task anlegen, API-Pods neu starten und Daten erneut abrufen" \
  "MEDIUM" \
  "2030-07-15"

echo

create_task \
  "Kubernetes-Begriffe wiederholen" \
  "Cluster, Node, Deployment, StatefulSet, Pod und Service erklären" \
  "LOW" \
  "2030-08-01"

echo

echo "Demo-Daten wurden angelegt."
echo "Abruf: curl '${BASE_URL}/api/tasks' | jq"
