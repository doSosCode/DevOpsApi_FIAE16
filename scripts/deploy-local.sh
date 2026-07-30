#!/usr/bin/env bash
set -euo pipefail

PROFILE="${MINIKUBE_PROFILE:-minikube}"
NAMESPACE="${K8S_NAMESPACE:-training}"
IMAGE_NAME="${IMAGE_NAME:-task-api}"
IMAGE_TAG="${IMAGE_TAG:-manual-$(date +%Y%m%d%H%M%S)}"

./scripts/preflight.sh
./gradlew clean test bootJar

K8S_NAMESPACE="${NAMESPACE}" IMAGE_NAME="${IMAGE_NAME}" ./scripts/tofu.sh apply
K8S_NAMESPACE="${NAMESPACE}" ./scripts/ensure-database-secret.sh

kubectl apply --namespace "${NAMESPACE}" --filename k8s/configmap.yaml
kubectl apply --namespace "${NAMESPACE}" --filename k8s/mariadb-service.yaml
kubectl apply --namespace "${NAMESPACE}" --filename k8s/mariadb-statefulset.yaml
kubectl rollout status --namespace "${NAMESPACE}" statefulset/mariadb --timeout=300s

minikube image build --profile "${PROFILE}" --tag "${IMAGE_NAME}:${IMAGE_TAG}" .
minikube image ls --profile "${PROFILE}" | grep -F "${IMAGE_NAME}:${IMAGE_TAG}"

kubectl apply --namespace "${NAMESPACE}" --filename k8s/service.yaml
kubectl apply --namespace "${NAMESPACE}" --filename k8s/pod-disruption-budget.yaml
sed "s|task-api:IMAGE_TAG|${IMAGE_NAME}:${IMAGE_TAG}|g" k8s/deployment.yaml \
  | kubectl apply --namespace "${NAMESPACE}" --filename -

# Lokales Zertifikat und HTTPS-Ingress gehören auch zum manuellen Komplettlauf.
K8S_NAMESPACE="${NAMESPACE}" ./scripts/ensure-tls-secret.sh
kubectl apply --namespace "${NAMESPACE}" --filename k8s/ingress.yaml

kubectl rollout status --namespace "${NAMESPACE}" deployment/task-api --timeout=300s

BASE_URL="http://$(minikube ip --profile "${PROFILE}"):30081" \
  ./scripts/smoke-test.sh
./scripts/smoke-test-https.sh

echo "HTTP:  http://$(minikube ip --profile "${PROFILE}"):30081/api/health"
echo "HTTPS: https://task-api.local/api/health"
