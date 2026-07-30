# Einrichtung und Vorführung der vollständigen DevOps-Schulungsumgebung

Diese Anleitung beschreibt den vollständigen Aufbau und die Vorführung des Projekts:

```text
GitHub Actions
+ Self-hosted Runner
+ Ansible Core
+ OpenTofu
+ Docker
+ Minikube
+ Kubernetes
+ Spring Boot
+ MariaDB
```

Sie ist für zwei Situationen gedacht:

1. **Empfohlener Weg:** Eine bereits vorbereitete Ubuntu-VM wird kopiert.
2. **Alternativer Weg:** Eine neue Ubuntu-VM wird vollständig eingerichtet.

---

# 1. Zielbild

Am Ende läuft folgende Architektur:

```text
GitHub Repository
        |
        | push / manueller Start
        v
GitHub Actions
        |
        v
Self-hosted Runner auf Ubuntu
        |
        +--> Ansible prüft den Host
        +--> OpenTofu verwaltet den Namespace
        +--> Gradle baut und testet die Anwendung
        +--> Docker/Minikube bauen das Task-API-Image
        +--> kubectl deployt MariaDB und Task-API
        |
        v
Minikube / Kubernetes
        |
        +--> MariaDB StatefulSet
        |      +--> PersistentVolumeClaim
        |
        +--> Task-API Deployment
        |      +--> zwei Pods
        |
        +--> NodePort Service 30081
```

Im Cluster laufen mindestens zwei Images:

```text
mariadb:11.4
task-api:<Git-Commit-SHA>
```

---

# 2. Empfohlener Weg: vorbereitete VM kopieren

## 2.1 Warum die VM kopieren?

Die Kopie enthält bereits:

- Java 21
- Docker
- kubectl
- Minikube
- OpenTofu
- Ansible Core
- Git
- einen vorbereiteten Benutzer
- gegebenenfalls einen GitHub Actions Runner

Dadurch bleibt der Unterrichtsaufbau schnell und reproduzierbar.

## 2.2 Vor dem Kopieren

Auf der Original-VM:

```bash
minikube stop -p minikube
sudo shutdown -h now
```

Die VM im ausgeschalteten Zustand kopieren.

## 2.3 Nach dem Kopieren

Auf der kopierten VM prüfen:

```bash
hostname
hostname -I
whoami
```

Optional einen neuen Hostnamen vergeben:

```bash
sudo hostnamectl set-hostname task-api-training
sudo reboot
```

Nach dem Neustart:

```bash
hostname
```

## 2.4 GitHub Runner auf der Kopie neu registrieren

Original-VM und Kopie dürfen nicht gleichzeitig dieselbe Runner-Identität verwenden.

Auf der VM-Kopie den alten Runner-Dienst entfernen:

```bash
cd ~/Dokumente/actions-runner
sudo ./svc.sh stop || true
sudo ./svc.sh uninstall || true
./config.sh remove
```

Falls der Runner an einem anderen Ort liegt:

```bash
find "$HOME" -maxdepth 3 -name config.sh -path '*actions-runner*'
```

Danach in GitHub:

```text
Repository
-> Settings
-> Actions
-> Runners
-> New self-hosted runner
-> Linux
-> x64
```

Die von GitHub angezeigten Befehle auf der kopierten VM ausführen.

Anschließend den Runner als Dienst installieren:

```bash
cd ~/Dokumente/actions-runner
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

## 2.5 Repository nicht im Runner-Arbeitsordner bearbeiten

Empfohlen:

```text
/home/devops/Projekte/task-api-training-repository
```

Nicht verwenden:

```text
/home/devops/Dokumente/actions-runner/_work
```

Der Ordner `_work` wird von GitHub Actions automatisch verwaltet.

---

# 3. Repository auf der VM bereitstellen

## 3.1 ZIP-Datei entpacken

```bash
mkdir -p ~/Projekte
cd ~/Projekte
unzip task-api-training-repository-audited.zip \
  -d task-api-training-repository
cd task-api-training-repository
```

## 3.2 Dateirechte setzen

```bash
chmod +x gradlew scripts/*.sh
```

## 3.3 Git-Repository initialisieren

```bash
git init
git add .
git commit -m "feat: add complete task API training environment"
git branch -M main
git remote add origin DEINE_GITHUB_REPOSITORY_ADRESSE
git push -u origin main
```

Beispiel:

```bash
git remote add origin \
  git@github.com:DEIN-BENUTZER/task-api-training.git
```

---

# 4. Vorhandene Werkzeuge prüfen

Das Repository bringt ein Preflight-Skript mit:

```bash
./scripts/preflight.sh
```

Zusätzlich Ansible verwenden:

```bash
export PATH="${HOME}/.local/bin:${PATH}"

ansible-playbook \
  -i ansible/inventory.ini \
  ansible/playbook.yml
```

Geprüft werden:

- Git
- Java
- Docker
- kubectl
- Minikube
- OpenTofu
- Docker-Zugriff
- Minikube-Status
- Kubernetes-Node

## 4.1 Erwartete Versionen anzeigen

```bash
git --version
java -version
docker --version
kubectl version --client
minikube version
tofu version
ansible-playbook --version
```

## 4.2 Docker-Zugriff prüfen

```bash
docker ps
```

Falls eine Berechtigungsfehlermeldung erscheint:

```bash
sudo usermod -aG docker "$USER"
```

Danach ab- und wieder anmelden.

---

# 5. Minikube vorbereiten

## 5.1 Bestehendes Minikube prüfen

```bash
minikube status -p minikube
```

## 5.2 Minikube starten

```bash
minikube start \
  --profile minikube \
  --driver docker \
  --cpus 4 \
  --memory 6144
```

## 5.3 Kubernetes-Verbindung prüfen

```bash
kubectl config current-context
kubectl get nodes
```

Erwartet:

```text
minikube
```

und ein Node mit Status:

```text
Ready
```

---

# 6. Ansible im Projekt

## 6.1 Aufgabe von Ansible

Ansible konfiguriert oder prüft den vorhandenen Rechner.

Im Projekt gibt es zwei Playbooks:

```text
ansible/playbook.yml
ansible/prepare-host.yml
```

## 6.2 `verify.yml`

Dieses Playbook verändert die VM nicht. Es prüft nur die Umgebung.

```bash
ansible-playbook \
  -i ansible/inventory.ini \
  ansible/playbook.yml
```

## 6.3 `prepare-host.yml`

Dieses Playbook ergänzt fehlende Basispakete auf einer neuen oder unvollständigen VM.

```bash
ansible-playbook \
  -i ansible/inventory.ini \
  ansible/prepare-host.yml \
  --ask-become-pass
```

Es installiert unter anderem:

- Git
- curl
- jq
- OpenSSL
- Java 21
- Docker

Es installiert bewusst nicht automatisch alle Spezialwerkzeuge. Minikube, kubectl, OpenTofu und Ansible sollten einmal kontrolliert eingerichtet werden.

## 6.4 Erklärung für die Klasse

```text
Ansible beschreibt den gewünschten Zustand eines Rechners.
Es kann Pakete installieren, Dienste starten und Konfigurationen prüfen.
```

---

# 7. OpenTofu im Projekt

## 7.1 Aufgabe von OpenTofu

OpenTofu verwaltet Infrastruktur als Code.

In diesem Projekt verwaltet OpenTofu den Kubernetes-Namespace:

```text
training
```

Dateien:

```text
infrastructure/versions.tf
infrastructure/variables.tf
infrastructure/main.tf
infrastructure/outputs.tf
```

## 7.2 OpenTofu initialisieren

Das Projekt enthält ein Hilfsskript, das den State dauerhaft außerhalb des Runner-Arbeitsordners speichert:

```bash
./scripts/tofu.sh init
```

## 7.3 Änderungen planen

```bash
./scripts/tofu.sh plan
```

## 7.4 Infrastruktur anwenden

```bash
./scripts/tofu.sh apply
```

## 7.5 Ergebnis prüfen

```bash
kubectl get namespace training
```

## 7.6 OpenTofu-State

Der State liegt dauerhaft hier:

```text
~/.local/state/task-api-training/terraform.tfstate
```

Das ist wichtig, weil der GitHub Actions Runner bei jedem Lauf ein neues Arbeitsverzeichnis verwendet.

## 7.7 Erklärung für die Klasse

```text
OpenTofu erstellt keine Anwendung.
Es beschreibt und verwaltet Infrastruktur.
Hier erzeugt es den Kubernetes-Namespace.
```

---

# 8. Anwendung lokal bauen und testen

## 8.1 Gradle-Build

```bash
./gradlew clean test bootJar
```

Dabei werden ausgeführt:

- Unit-Tests
- Controller-Tests
- MariaDB-Integrationstest über Testcontainers
- Erstellung der ausführbaren JAR-Datei

## 8.2 Testberichte

```text
build/reports/tests/test/index.html
```

## 8.3 Testarten erklären

```text
TaskServiceTest
-> prüft Fachlogik ohne HTTP und ohne echte Datenbank

TaskControllerTest
-> prüft HTTP, Validierung und Fehlerantworten

TaskApiMariaDbIntegrationTest
-> startet eine echte temporäre MariaDB mit Testcontainers

smoke-test.sh
-> prüft die vollständig deployte Anwendung
```

---

# 9. Schnelle Demonstration mit Docker Compose

Docker Compose ist der einfachste Einstieg vor Kubernetes.

## 9.1 Umgebungsdatei erstellen

```bash
cp .env.example .env
```

Sichere Zufallswerte erzeugen:

```bash
DB_PASSWORD="$(openssl rand -hex 32)"
DB_ROOT_PASSWORD="$(openssl rand -hex 40)"

sed -i "s/CHANGE_ME_WITH_A_LONG_RANDOM_VALUE/${DB_PASSWORD}/" .env
sed -i "s/CHANGE_ME_WITH_ANOTHER_LONG_RANDOM_VALUE/${DB_ROOT_PASSWORD}/" .env
```

## 9.2 Anwendung starten

```bash
./gradlew clean test bootJar
docker compose up --build
```

## 9.3 Zugriff

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/tasks
```

## 9.4 Task anlegen

```bash
curl -X POST \
  http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Docker Compose demonstrieren",
    "description": "Task wird in MariaDB gespeichert",
    "priority": "HIGH",
    "dueDate": "2030-12-31"
  }'
```

## 9.5 Umgebung beenden

```bash
docker compose down
```

Datenbankdaten ebenfalls löschen:

```bash
docker compose down --volumes
```

## 9.6 Erklärung für die Klasse

```text
Docker Compose startet mehrere Container gemeinsam:
- Task-API
- MariaDB

Die Anwendung greift über den Servicenamen mariadb auf die Datenbank zu.
```

---

# 10. Vollständiges Deployment nach Minikube

## 10.1 Automatisches lokales Deployment

```bash
./scripts/deploy-local.sh
```

Das Skript führt aus:

```text
1. Umgebung prüfen
2. Anwendung testen und bauen
3. Namespace mit OpenTofu verwalten
4. Datenbank-Secret erzeugen
5. MariaDB deployen
6. MariaDB-Rollout abwarten
7. Task-API-Image in Minikube bauen
8. Service und Deployment anwenden
9. Task-API-Rollout abwarten
10. Smoke-Test ausführen
```

## 10.2 Kubernetes-Zustand prüfen

```bash
kubectl get all -n training
kubectl get pvc -n training
kubectl get pods -n training -o wide
```

## 10.3 Erwartete Ressourcen

```text
StatefulSet: mariadb
Deployment:  task-api
Service:     mariadb
Service:     task-api
PVC:         data-mariadb-0
Pods:        ein MariaDB-Pod und zwei Task-API-Pods
```

## 10.4 Anwendung aufrufen

```bash
MINIKUBE_IP="$(minikube ip -p minikube)"

curl "http://${MINIKUBE_IP}:30081/api/health"
curl "http://${MINIKUBE_IP}:30081/api/tasks"
```

---

# 11. GitHub Actions konfigurieren

## 11.1 Runner prüfen

In GitHub:

```text
Repository
-> Settings
-> Actions
-> Runners
```

Der Runner muss als `Idle` oder während eines Jobs als `Active` erscheinen.

## 11.2 Optionale GitHub Secrets

In GitHub:

```text
Repository
-> Settings
-> Secrets and variables
-> Actions
-> New repository secret
```

Optional anlegen:

```text
TRAINING_DB_PASSWORD
TRAINING_DB_ROOT_PASSWORD
```

Zufällige Werte erzeugen:

```bash
openssl rand -hex 32
openssl rand -hex 40
```

Wenn keine Secrets gesetzt sind, erzeugt das Skript beim ersten Deployment zufällige Werte und legt sie als Kubernetes Secret an.

## 11.3 Pipeline auslösen

Durch Push:

```bash
git add .
git commit -m "feat: demonstrate complete CI/CD pipeline"
git push
```

Oder manuell:

```text
GitHub
-> Actions
-> Task API CI/CD
-> Run workflow
```

## 11.4 Pipeline-Aufteilung

### Verify-Job

Läuft auf einem GitHub-gehosteten Runner:

```text
Checkout
-> Java einrichten
-> Gradle Build
-> Tests
-> bootJar
```

### Deploy-Job

Läuft nur bei Push oder manuellem Start auf dem Self-hosted Runner:

```text
VM prüfen
-> Ansible verify
-> JAR bauen
-> OpenTofu apply
-> Secret sicherstellen
-> MariaDB deployen
-> API-Image bauen
-> API deployen
-> Rollout
-> Smoke-Test
```

Pull-Request-Code wird bewusst nicht auf dem privilegierten Self-hosted Runner ausgeführt.

---

# 12. Datenbank demonstrieren

## 12.1 Task anlegen

```bash
BASE_URL="http://$(minikube ip -p minikube):30081"

curl -X POST \
  "${BASE_URL}/api/tasks" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Persistenz testen",
    "description": "Dieser Task bleibt nach einem API-Rollout erhalten",
    "priority": "MEDIUM",
    "dueDate": "2030-12-31"
  }'
```

## 12.2 Tasks anzeigen

```bash
curl "${BASE_URL}/api/tasks"
```

## 12.3 API-Pods neu starten

```bash
kubectl rollout restart deployment/task-api -n training
kubectl rollout status deployment/task-api -n training
```

## 12.4 Daten erneut abrufen

```bash
curl "${BASE_URL}/api/tasks"
```

Die Daten bleiben bestehen, weil MariaDB sie im PersistentVolumeClaim speichert.

---

# 13. Mehrere Images und Rolling Update demonstrieren

## 13.1 Aktuell laufende Images anzeigen

```bash
kubectl get pods -n training \
  -o custom-columns='POD:.metadata.name,IMAGE:.spec.containers[*].image'
```

## 13.2 Kleine Codeänderung durchführen

Beispielsweise eine Antwort oder einen Logtext ändern.

Danach:

```bash
git add .
git commit -m "feat: demonstrate rolling update"
git push
```

Die Pipeline erzeugt einen neuen unveränderlichen Image-Tag:

```text
task-api:<erste 12 Zeichen der Commit-SHA>
```

## 13.3 Rollout beobachten

```bash
kubectl get pods -n training -w
```

In einem zweiten Terminal:

```bash
kubectl rollout status deployment/task-api -n training
```

## 13.4 ReplicaSets anzeigen

```bash
kubectl get replicasets -n training
```

Damit lässt sich erklären:

```text
- alte und neue Version existieren während des Rollouts kurz gleichzeitig
- Kubernetes ersetzt die Pods schrittweise
- die Datenbank bleibt unverändert
```

---

# 14. Sicherheitsmaßnahmen erklären

Das Projekt verwendet auch ohne HTTPS mehrere Schutzmaßnahmen.

## 14.1 Container-Sicherheit

```text
- Anwendung läuft nicht als root
- read-only Root-Dateisystem
- keine zusätzlichen Linux Capabilities
- keine Privilege Escalation
- Seccomp RuntimeDefault
- Ressourcenlimits
- Service-Account-Token deaktiviert
```

Prüfen:

```bash
kubectl get deployment task-api -n training -o yaml
```

## 14.2 Secret-Verwaltung

```text
- Kennwörter stehen nicht im Git-Repository
- Kubernetes Secret enthält Zugangsdaten
- GitHub Secrets können die Werte liefern
```

Nur Metadaten anzeigen:

```bash
kubectl get secret task-api-database -n training
```

Secret-Werte nicht im Unterricht offen ausgeben.

## 14.3 API-Sicherheit

```text
- Eingabevalidierung
- begrenzte Listenabfragen
- keine Stacktraces in Antworten
- standardisierte Fehlerantworten
- Security Header
- Request-ID
- optimistisches Locking
```

## 14.4 Was bewusst fehlt

```text
- HTTPS/TLS
- Benutzeranmeldung
- Rollen und Berechtigungen
- produktives Secrets Management
- produktiver Ingress
```

Die spätere Authentifizierung ist dokumentiert in:

```text
docs/AUTHENTICATION_EXTENSION.md
```

---

# 15. Empfohlener Ablauf für die Unterrichtsvorstellung

## Teil 1 – Projektübersicht, etwa 5 Minuten

Zeigen:

```text
README.md
Repository-Struktur
Dockerfile
compose.yaml
k8s/
ansible/
infrastructure/
.github/workflows/
```

Kernaussage:

```text
Ein Repository enthält Anwendung, Tests, Infrastruktur und Automatisierung.
```

## Teil 2 – Anwendung und Tests, etwa 10 Minuten

```bash
./gradlew clean test bootJar
```

Erklären:

- Unit-Test
- Controller-Test
- Integrationstest mit echter MariaDB
- auslieferbares JAR

## Teil 3 – Docker Compose, etwa 10 Minuten

```bash
docker compose up --build
```

Erklären:

- zwei Images
- internes Docker-Netzwerk
- Portweiterleitung
- MariaDB-Persistenz

## Teil 4 – Ansible und OpenTofu, etwa 10 Minuten

```bash
ansible-playbook -i ansible/inventory.ini ansible/playbook.yml
./scripts/tofu.sh plan
./scripts/tofu.sh apply
```

Erklären:

```text
Ansible konfiguriert und prüft Rechner.
OpenTofu verwaltet Infrastrukturzustand.
```

## Teil 5 – Kubernetes, etwa 15 Minuten

```bash
./scripts/deploy-local.sh
kubectl get all -n training
kubectl get pvc -n training
```

Erklären:

- Cluster
- Node
- Deployment
- StatefulSet
- Pod
- Service
- PVC
- Secret
- ConfigMap

## Teil 6 – API und Datenbank, etwa 10 Minuten

Task anlegen, lesen, ändern und löschen.

Anschließend API-Pods neu starten und zeigen, dass die Daten erhalten bleiben.

## Teil 7 – GitHub Actions, etwa 10 Minuten

Eine kleine Änderung committen und pushen.

In GitHub zeigen:

- Verify-Job
- Deploy-Job
- Self-hosted Runner
- neues Image
- Rolling Update
- Smoke-Test

## Teil 8 – Sicherheit, etwa 5 Minuten

Erklären:

- keine Kennwörter im Repository
- Non-Root
- Read-only Root-Dateisystem
- minimale Workflow-Rechte
- Pull Requests nicht auf Self-hosted Runner
- Ressourcenlimits

---

# 16. Kurzer Spickzettel für Rückfragen

## „Warum Ansible und OpenTofu?“

```text
Ansible konfiguriert Rechner.
OpenTofu verwaltet Infrastruktur.
```

## „Warum MariaDB als StatefulSet?“

```text
Die Datenbank benötigt eine stabile Identität und persistenten Speicher.
```

## „Warum läuft die API als Deployment?“

```text
Die API ist zustandslos und kann mit mehreren austauschbaren Pods laufen.
```

## „Warum zwei Task-API-Pods?“

```text
Damit Skalierung und Rolling Updates sichtbar werden.
```

## „Warum ein Service?“

```text
Pods können ersetzt werden. Der Service stellt einen stabilen Zugang bereit.
```

## „Warum Image-Tags mit Commit-SHA?“

```text
Jede Version ist eindeutig und nachvollziehbar.
```

## „Warum keine Passwörter im YAML?“

```text
Geheimnisse gehören nicht in die Versionsverwaltung.
```

## „Warum läuft der Pull Request nicht auf dem Self-hosted Runner?“

```text
Ungeprüfter Code soll keinen Zugriff auf Docker, Minikube und den Host erhalten.
```

---

# 17. Fehlerbehebung

## Runner offline

```bash
cd ~/Dokumente/actions-runner
sudo ./svc.sh status
sudo ./svc.sh start
```

## Minikube nicht gestartet

```bash
minikube start -p minikube --driver docker
```

## Docker-Berechtigung fehlt

```bash
sudo usermod -aG docker "$USER"
```

Danach neu anmelden.

## Pod startet nicht

```bash
kubectl get pods -n training
kubectl describe pod PODNAME -n training
kubectl logs PODNAME -n training
kubectl logs PODNAME -n training --previous
```

## Image fehlt

```bash
minikube image ls -p minikube | grep task-api
```

## Datenbank nicht bereit

```bash
kubectl get statefulset,pods,service,pvc -n training
kubectl logs statefulset/mariadb -n training
```

## OpenTofu meldet vorhandenen Namespace

Prüfen:

```bash
./scripts/tofu.sh plan
```

State-Datei:

```text
~/.local/state/task-api-training/terraform.tfstate
```

Falls der Namespace manuell erstellt wurde und noch nicht im State ist, zunächst sichern und anschließend importieren:

```bash
./scripts/tofu.sh import \
  kubernetes_namespace_v1.training \
  training
```

## API nicht erreichbar

```bash
minikube ip -p minikube
kubectl get service task-api -n training
curl "http://$(minikube ip -p minikube):30081/api/health"
```

---

# 18. Umgebung aufräumen

## Anwendung entfernen, Daten behalten

```bash
./scripts/cleanup.sh
```

## Anwendung und Daten vollständig entfernen

```bash
DELETE_DATA=true ./scripts/cleanup.sh
```

## Minikube vollständig löschen

```bash
minikube delete -p minikube
```

Danach sind Cluster, Pods, Services und lokale Minikube-Images entfernt.

---

# 19. Frische Ubuntu-VM vollständig vorbereiten

Dieser Abschnitt ist nur notwendig, wenn keine vorbereitete VM verwendet wird.

## 19.1 Basispakete

```bash
sudo apt-get update
sudo apt-get install -y \
  ca-certificates \
  curl \
  git \
  jq \
  openssl \
  unzip \
  pipx \
  openjdk-21-jdk \
  docker.io
```

## 19.2 Docker

```bash
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

Danach neu anmelden.

## 19.3 Ansible Core

```bash
pipx install ansible-core
pipx ensurepath
export PATH="${HOME}/.local/bin:${PATH}"
```

## 19.4 kubectl

```bash
ARCH="$(dpkg --print-architecture)"
case "$ARCH" in
  amd64) KARCH=amd64 ;;
  arm64) KARCH=arm64 ;;
  *) echo "Nicht unterstützte Architektur: $ARCH"; exit 1 ;;
esac

KUBECTL_VERSION="$(curl -fsSL https://dl.k8s.io/release/stable.txt)"
curl -fsSLo /tmp/kubectl \
  "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${KARCH}/kubectl"
sudo install -m 0755 /tmp/kubectl /usr/local/bin/kubectl
```

## 19.5 Minikube

```bash
curl -fsSLo /tmp/minikube \
  "https://storage.googleapis.com/minikube/releases/latest/minikube-linux-${KARCH}"
sudo install -m 0755 /tmp/minikube /usr/local/bin/minikube
```

## 19.6 OpenTofu

Die Installation sollte nach der aktuellen offiziellen OpenTofu-Anleitung erfolgen. Danach prüfen:

```bash
tofu version
```

## 19.7 Minikube starten

```bash
minikube start \
  -p minikube \
  --driver docker \
  --cpus 4 \
  --memory 6144
```

## 19.8 GitHub Runner

In GitHub einen neuen Self-hosted Runner erzeugen und die angezeigten Befehle ausführen.

---

# 20. Abschlusskontrolle vor dem Unterricht

Am Tag vor der Vorstellung ausführen:

```bash
cd ~/Projekte/task-api-training-repository

chmod +x gradlew scripts/*.sh
./scripts/preflight.sh
ansible-playbook -i ansible/inventory.ini ansible/playbook.yml
./scripts/tofu.sh plan
./gradlew clean test bootJar
./scripts/deploy-local.sh
```

Danach prüfen:

```bash
kubectl get all -n training
kubectl get pvc -n training
curl "http://$(minikube ip -p minikube):30081/api/health"
curl "http://$(minikube ip -p minikube):30081/api/tasks"
```

GitHub Runner prüfen:

```bash
cd ~/Dokumente/actions-runner
sudo ./svc.sh status
```

Damit ist die Umgebung bereit für die vollständige Präsentation.
