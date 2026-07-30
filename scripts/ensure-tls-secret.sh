#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${K8S_NAMESPACE:-training}"
TLS_HOSTNAME="${TLS_HOSTNAME:-task-api.local}"
TLS_DIR="${TLS_DIR:-${HOME}/.local/share/task-api-training/tls}"
SECRET_NAME="${TLS_SECRET_NAME:-task-api-tls}"
CA_DAYS="${CA_DAYS:-3650}"
CERT_DAYS="${CERT_DAYS:-825}"

mkdir -p "${TLS_DIR}"
chmod 700 "${TLS_DIR}"

CA_KEY="${TLS_DIR}/training-ca.key"
CA_CERT="${TLS_DIR}/training-ca.crt"
SERVER_KEY="${TLS_DIR}/${TLS_HOSTNAME}.key"
SERVER_CSR="${TLS_DIR}/${TLS_HOSTNAME}.csr"
SERVER_CERT="${TLS_DIR}/${TLS_HOSTNAME}.crt"
OPENSSL_CONFIG="${TLS_DIR}/${TLS_HOSTNAME}.cnf"

# Lokale CA und private Schlüssel werden nie in Git gespeichert.
if [[ ! -f "${CA_KEY}" || ! -f "${CA_CERT}" ]]; then
  openssl genrsa -out "${CA_KEY}" 4096
  chmod 600 "${CA_KEY}"
  openssl req -x509 -new -sha256 \
    -key "${CA_KEY}" \
    -days "${CA_DAYS}" \
    -subj "/C=DE/O=Task API Training/CN=Task API Training Local CA" \
    -out "${CA_CERT}"
  chmod 644 "${CA_CERT}"
fi

cat > "${OPENSSL_CONFIG}" <<CONFIG
[req]
prompt = no
distinguished_name = dn
req_extensions = req_ext

[dn]
C = DE
O = Task API Training
CN = ${TLS_HOSTNAME}

[req_ext]
subjectAltName = @alt_names
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[alt_names]
DNS.1 = ${TLS_HOSTNAME}
DNS.2 = task-api
DNS.3 = task-api.${NAMESPACE}.svc
DNS.4 = task-api.${NAMESPACE}.svc.cluster.local
CONFIG

# Serverzertifikat erneuern, wenn es fehlt oder in weniger als 30 Tagen abläuft.
if [[ ! -f "${SERVER_KEY}" || ! -f "${SERVER_CERT}" ]] \
  || ! openssl x509 -checkend 2592000 -noout -in "${SERVER_CERT}"; then
  openssl genrsa -out "${SERVER_KEY}" 3072
  chmod 600 "${SERVER_KEY}"
  openssl req -new -sha256 \
    -key "${SERVER_KEY}" \
    -config "${OPENSSL_CONFIG}" \
    -out "${SERVER_CSR}"
  openssl x509 -req -sha256 \
    -in "${SERVER_CSR}" \
    -CA "${CA_CERT}" \
    -CAkey "${CA_KEY}" \
    -CAcreateserial \
    -days "${CERT_DAYS}" \
    -extensions req_ext \
    -extfile "${OPENSSL_CONFIG}" \
    -out "${SERVER_CERT}"
  chmod 644 "${SERVER_CERT}"
  rm -f "${SERVER_CSR}"
fi

kubectl create secret tls "${SECRET_NAME}" \
  --namespace "${NAMESPACE}" \
  --cert "${SERVER_CERT}" \
  --key "${SERVER_KEY}" \
  --dry-run=client \
  --output yaml \
  | kubectl apply --filename -

echo "TLS-Secret '${SECRET_NAME}' wurde bereitgestellt."
echo "Lokales CA-Zertifikat: ${CA_CERT}"
