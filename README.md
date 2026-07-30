# Task API Training Platform

Lokale DevOps-Demonstration fuer eine minimale Ubuntu-VM unter Hyper-V. Nach
der einmaligen Installation und Registrierung eines GitHub Self-hosted Runners
werden Host-Konfiguration, Tests, Infrastruktur, Datenbank, Container und
Deployment automatisiert.

## Gesamtarchitektur

```text
GitHub Repository
       |
       v
GitHub Actions
  |-- 01 Bootstrap Ubuntu VM
  |     `-- Ansible installiert und konfiguriert
  |           Git, Java 21, Docker, kubectl, Minikube und OpenTofu
  |
  `-- 02 Build, Test and Deploy
        |-- Gradle baut und testet die Task-API
        |-- OpenTofu verwaltet den Namespace
        |-- Docker baut task-api:<Commit-SHA>
        `-- Kubernetes betreibt
              |-- MariaDB mit persistentem Speicher
              |-- zwei Task-API-Pods
              `-- NGINX Ingress mit lokalem TLS-Zertifikat
```

## Einmalig manuell erforderlich

1. Ubuntu-VM in Hyper-V anlegen.
2. Netzwerk, SSH, `curl`, Git und CA-Zertifikate bereitstellen.
3. Ein leeres GitHub-Repository anlegen und dieses Projekt dorthin pushen.
4. Self-hosted Runner auf der VM registrieren.
5. Workflow `01 - Bootstrap Ubuntu VM` mit Git-Name und Git-E-Mail starten.
6. VM einmal neu starten.
7. Optional persönlichen SSH-Schluessel fuer manuelle Git-Pushes einrichten.
8. Workflow `02 - Build, Test and Deploy` starten.

Vollstaendige Befehle: [`docs/DOZENTEN_ANLEITUNG.md`](docs/DOZENTEN_ANLEITUNG.md)

## Automatisierter Ablauf

```text
Ansible prueft und startet Minikube
-> Gradle kompiliert und testet
-> OpenTofu erstellt bzw. verwaltet Namespace training
-> Kubernetes erzeugt Secret, MariaDB StatefulSet und PVC
-> Docker baut das Task-API-Image
-> Kubernetes rollt zwei API-Pods aus
-> lokale CA, TLS-Secret und HTTPS-Ingress
-> HTTP- und HTTPS-Smoke-Test
```

## Git und Versionierung

Der Bootstrap-Workflow erzeugt `~/.gitconfig` fuer den Runner-Benutzer. Der
persoenliche GitHub-Zugang fuer manuelle Pushes wird getrennt per SSH-Schluessel
eingerichtet. Der Runner verwendet fuer `actions/checkout` ein kurzlebiges
Workflow-Token und erstellt keine automatischen Commits.

Details: [`docs/GIT_VERSIONIERUNG_UND_ZUGRIFF.md`](docs/GIT_VERSIONIERUNG_UND_ZUGRIFF.md)

## Images

```text
mariadb:11.4
task-api:<Git-Commit-SHA>
```

MariaDB wird nicht als Ubuntu-Paket installiert. Kubernetes startet das
MariaDB-Image. Das Image erzeugt Datenbank und Benutzer; Flyway erzeugt beim
Start der API die Tabellen.

## Projektstruktur

```text
.github/workflows/   Bootstrap und CI/CD
ansible/             Host-Installation, Git-Konfiguration und Pruefung
infrastructure/      OpenTofu-Konfiguration
k8s/                 Kubernetes-Ressourcen und Ingress
scripts/             Bootstrap-, Git-, Betriebs- und Testskripte
src/                 Spring-Boot-Code und Tests
docs/                Dozenten-, Git-, API-, Sicherheits- und TLS-Dokumentation
```

## Zugriff

HTTP:

```bash
curl "http://$(minikube ip -p minikube):30081/api/health"
```

HTTPS:

```bash
curl \
  --cacert ~/.local/share/task-api-training/tls/training-ca.crt \
  --resolve "task-api.local:443:$(minikube ip -p minikube)" \
  https://task-api.local/api/health
```

## Sicherheitsentscheidungen

- Pull Requests laufen nicht auf dem privilegierten Self-hosted Runner.
- Workflow-Checkout speichert keine GitHub-Zugangsdaten dauerhaft.
- Datenbankkennwoerter und private TLS-/SSH-Schluessel liegen nicht im Repository.
- API-Container laeuft als Non-Root mit Read-only Root-Dateisystem.
- Linux Capabilities werden entfernt.
- Health-Probes, Ressourcenlimits und versionierte Flyway-Migrationen sind definiert.
- Image-Tags basieren auf dem Git-Commit.
- Die lokale CA ist nur fuer die isolierte Schulungsumgebung vorgesehen.

## Dokumentation

- [`docs/DOZENTEN_ANLEITUNG.md`](docs/DOZENTEN_ANLEITUNG.md)
- [`docs/GIT_VERSIONIERUNG_UND_ZUGRIFF.md`](docs/GIT_VERSIONIERUNG_UND_ZUGRIFF.md)
- [`docs/ZERTIFIKATE_UND_HTTPS.md`](docs/ZERTIFIKATE_UND_HTTPS.md)
- [`docs/API.md`](docs/API.md)
- [`docs/ARCHITECTURE_AND_SECURITY.md`](docs/ARCHITECTURE_AND_SECURITY.md)
