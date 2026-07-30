# Lokale Zertifikate und HTTPS

Die Schulungsumgebung verwendet eine lokale Zertifizierungsstelle (CA). Private
Schlüssel werden nicht im Repository gespeichert, sondern unter:

```text
~/.local/share/task-api-training/tls
```

## Automatischer Ablauf

Die Pipeline:

1. erzeugt einmalig eine lokale CA,
2. erzeugt ein Zertifikat für `task-api.local`,
3. erstellt das Kubernetes-Secret `task-api-tls`,
4. deployt einen NGINX Ingress,
5. prüft HTTPS mit vollständiger Zertifikatsprüfung.

`curl -k` wird bewusst nicht verwendet.

## Ubuntu-VM vorbereiten

```bash
echo "$(minikube ip -p minikube) task-api.local" \
  | sudo tee -a /etc/hosts

sudo cp ~/.local/share/task-api-training/tls/training-ca.crt \
  /usr/local/share/ca-certificates/task-api-training-ca.crt
sudo update-ca-certificates

curl https://task-api.local/api/health
```

## Windows-Hyper-V-Host vorbereiten

CA exportieren:

```bash
./scripts/export-training-ca.sh
```

Datei per SCP auf Windows kopieren. PowerShell als Administrator:

```powershell
Import-Certificate `
  -FilePath .\task-api-training-ca.crt `
  -CertStoreLocation Cert:\LocalMachine\Root
```

In `C:\Windows\System32\drivers\etc\hosts` eintragen:

```text
MINIKUBE_IP task-api.local
```

Danach:

```text
https://task-api.local/api/health
```

Bei Hyper-V-NAT kann die Minikube-IP vom Windows-Host nicht direkt erreichbar
sein. Dann einen externen Hyper-V-Switch verwenden oder für HTTP temporär
`scripts/start-port-forward.sh` starten.

Die lokale CA ist nur für die isolierte Schulungsumgebung gedacht. In Produktion
würde man eine Unternehmens-PKI oder ACME/cert-manager verwenden.
