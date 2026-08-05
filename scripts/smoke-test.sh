#!/usr/bin/env bash
set -euo pipefail

PROFILE="${MINIKUBE_PROFILE:-minikube}"
BASE_URL="${BASE_URL:-http://$(minikube ip --profile "${PROFILE}"):30081}"

response="$(curl --fail --silent --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{"title":"Pipeline-Smoke-Test","description":"Automatisch erzeugt","priority":"MEDIUM","dueDate":"2030-12-31"}' \
  "${BASE_URL}/api/tasks")"

id="$(printf '%s' "${response}" | jq -r '.id')"
test "${id}" != "null"

curl --fail --silent --request PATCH \
  --header 'Content-Type: application/json' \
  --data '{"completed":true}' \
  "${BASE_URL}/api/tasks/${id}/completion" | jq .

echo "Smoke-Test erfolgreich."
