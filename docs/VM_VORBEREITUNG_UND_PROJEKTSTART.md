# Schritt-für-Schritt: VM vorbereiten, Repository anpassen und Projekt starten

Diese Anleitung ist für die bereits vorhandene beziehungsweise kopierte Ubuntu-VM gedacht.

Zielsystem:

```text
Ubuntu-VM
├── GitHub Self-hosted Runner
├── Java 21
├── Docker
├── Minikube
├── kubectl
├── Ansible Core
├── OpenTofu
└── Repository
    ├── Spring-Boot-Task-API
    ├── MariaDB
    ├── Tests
    └── GitHub-Actions-Pipeline
```

---

## 1. Werte festlegen

Für die Anleitung werden folgende Standardwerte verwendet:

```text
Linux-Benutzer:      devops
Projektordner:       /home/devops/Projekte/task-api-training
Runner-Ordner:       /home/devops/Dokumente/actions-runner
Minikube-Profil:     minikube
Kubernetes-Namespace: training
Anwendungsname:      task-api
Datenbankname:       taskdb
Datenbankbenutzer:   taskapp
API-Port im Container: 8080
Kubernetes-NodePort: 30081
```

Passe die Pfade an, falls deine VM andere Verzeichnisse verwendet.

---

# Teil A – Kopierte VM vorbereiten

## 2. VM starten und Grundzustand prüfen

```bash
hostnamectl
ip address
whoami
pwd
```

Prüfe insbesondere:

- stimmt der Hostname,
- besitzt die VM eine IP-Adresse,
- bist du mit dem richtigen Benutzer angemeldet,
- hat die VM Internetzugriff,
- sind mindestens 4 CPU-Kerne und 6–8 GB RAM verfügbar.

Internet testen:

```bash
curl -I https://github.com
```

Speicher prüfen:

```bash
free -h
df -h
```

---

## 3. Hostname der Kopie ändern

Eine VM-Kopie sollte einen eigenen Hostnamen besitzen.

```bash
sudo hostnamectl set-hostname task-api-training-vm
```

Danach entweder neu anmelden oder die VM neu starten:

```bash
sudo reboot
```

---

## 4. GitHub Runner auf der Kopie neu registrieren

Original-VM und Kopie dürfen nicht gleichzeitig dieselbe Runner-Registrierung verwenden.

Auf der kopierten VM:

```bash
cd ~/Dokumente/actions-runner
sudo ./svc.sh stop || true
sudo ./svc.sh uninstall || true
./config.sh remove
```

Falls `config.sh remove` nach einem Token fragt, erstelle in GitHub ein neues Removal-Token oder entferne den alten Runner über:

```text
GitHub Repository
→ Settings
→ Actions
→ Runners
```

Danach einen neuen Runner hinzufügen:

```text
GitHub Repository
→ Settings
→ Actions
→ Runners
→ New self-hosted runner
```

Die von GitHub angezeigten Befehle auf der VM ausführen.

Anschließend den Runner wieder als Dienst starten:

```bash
cd ~/Dokumente/actions-runner
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

In GitHub muss der Runner als **Idle** erscheinen.

---

## 5. Werkzeuge auf der VM prüfen

```bash
git --version
java -version
docker --version
kubectl version --client
minikube version
tofu version
ansible-playbook --version
curl --version
jq --version
openssl version
```

Fehlt ein Werkzeug, führe zuerst das vorbereitende Ansible-Playbook aus, soweit Ansible bereits vorhanden ist:

```bash
cd ~/Projekte/task-api-training
ansible-playbook \
  -i ansible/inventory.ini \
  ansible/prepare-host.yml \
  --ask-become-pass
```

`prepare-host.yml` installiert die Basispakete, aber nicht automatisch Minikube, kubectl, OpenTofu oder Ansible selbst. Diese Werkzeuge sollten auf der kopierten VM bereits vorhanden sein.

---

## 6. Docker-Berechtigung prüfen

```bash
docker ps
```

Erscheint ein Berechtigungsfehler:

```bash
sudo usermod -aG docker "$USER"
```

Danach abmelden und neu anmelden oder einmalig:

```bash
newgrp docker
```

Erneut testen:

```bash
docker ps
docker run --rm hello-world
```

---

## 7. Minikube prüfen und starten

Status prüfen:

```bash
minikube status -p minikube
```

Falls Minikube gestoppt ist:

```bash
minikube start \
  -p minikube \
  --driver docker \
  --cpus 4 \
  --memory 6144
```

Kubernetes prüfen:

```bash
kubectl config current-context
kubectl get nodes
```

Erwartung:

```text
Kontext: minikube
Node: Ready
```

Falls der falsche Kontext aktiv ist:

```bash
kubectl config use-context minikube
```

---

# Teil B – Repository auf der VM einrichten

## 8. Repository außerhalb des Runner-Arbeitsordners ablegen

Nicht im Verzeichnis `actions-runner/_work` entwickeln.

Empfohlener Ort:

```bash
mkdir -p ~/Projekte
cd ~/Projekte
```

ZIP entpacken:

```bash
unzip task-api-training-repository-audited-mit-anleitung.zip \
  -d task-api-training

cd task-api-training
```

Oder aus GitHub klonen:

```bash
git clone DEINE_REPOSITORY_ADRESSE task-api-training
cd task-api-training
```

Dateirechte setzen:

```bash
chmod +x gradlew scripts/*.sh
```

---

## 9. Repository an dein System anpassen

### 9.1 GitHub-Repository-Adresse

Nur nötig, wenn das Verzeichnis noch kein korrektes Git-Remote besitzt:

```bash
git remote -v
```

Remote setzen oder ändern:

```bash
git remote remove origin 2>/dev/null || true
git remote add origin DEINE_GITHUB_REPOSITORY_ADRESSE
```

Beispiel:

```bash
git remote add origin git@github.com:DEIN-NAME/task-api-training.git
```

### 9.2 Runner-Labels

Datei:

```text
.github/workflows/ci-cd.yml
```

Standard:

```yaml
runs-on: [self-hosted, linux, x64]
```

Prüfe in GitHub, ob dein Runner diese Labels besitzt. Bei ARM64 muss `x64` entsprechend geändert werden.

### 9.3 Namespace

Standard:

```text
training
```

Stellen, die bei einer Änderung gemeinsam angepasst werden müssen:

```text
.github/workflows/ci-cd.yml
infrastructure/terraform.tfvars.example
scripts/tofu.sh
scripts/deploy-local.sh
scripts/cleanup.sh
scripts/ensure-database-secret.sh
```

Am einfachsten bleibt der Standardwert `training` bestehen.

### 9.4 Anwendungsname und Containername

Standard:

```text
task-api
```

Bei einer Umbenennung müssen mindestens angepasst werden:

```text
.github/workflows/ci-cd.yml
k8s/deployment.yaml
k8s/service.yaml
k8s/configmap.yaml
k8s/pod-disruption-budget.yaml
scripts/deploy-local.sh
infrastructure/terraform.tfvars.example
```

Für die erste Vorführung sollte der Name unverändert bleiben.

### 9.5 Datenbankname und Benutzer

Standard:

```text
Datenbank: taskdb
Benutzer:  taskapp
```

Bei Änderungen müssen diese Werte konsistent sein in:

```text
.env
k8s/configmap.yaml
scripts/ensure-database-secret.sh
```

Beispiel in `k8s/configmap.yaml`:

```yaml
DB_URL: jdbc:mariadb://mariadb:3306/taskdb
```

### 9.6 Ports

Standard:

```text
Spring Boot im Container: 8080
NodePort in Kubernetes:    30081
Docker-Compose-Hostport:   8080
```

Bei einer NodePort-Änderung anpassen:

```text
k8s/service.yaml
scripts/deploy-local.sh
scripts/smoke-test.sh
README.md
```

NodePorts liegen üblicherweise im Bereich `30000–32767`.

### 9.7 Health-Endpunkt

Standard:

```text
/api/health
```

Falls er geändert wird, anpassen in:

```text
k8s/deployment.yaml
scripts/smoke-test.sh
.github/workflows/ci-cd.yml
README.md
```

---

## 10. Projektkonfiguration prüfen

```bash
./scripts/vm-clone-checklist.sh
./scripts/preflight.sh
```

Danach Ansible-Prüfung:

```bash
ansible-playbook \
  -i ansible/inventory.ini \
  ansible/playbook.yml
```

Diese Prüfung verändert das System nicht. Sie kontrolliert nur:

- Java,
- Docker,
- kubectl,
- Minikube,
- OpenTofu,
- Docker-Zugriff,
- Minikube-Status,
- Kubernetes-Node.

---

# Teil C – Datenbank vorbereiten

## 11. Muss MariaDB manuell installiert werden?

Nein.

MariaDB läuft als Container:

```text
Docker Compose: mariadb:11.4
Kubernetes:     mariadb:11.4 als StatefulSet
```

Auf der Ubuntu-VM muss kein lokaler MariaDB-Server installiert werden.

---

## 12. Muss das Datenbankschema manuell erstellt werden?

Nein.

Beim Start der Task-API führt **Flyway** automatisch diese Migration aus:

```text
src/main/resources/db/migration/V1__create_tasks_table.sql
```

Flyway erzeugt:

- Tabelle `tasks`,
- Primärschlüssel,
- Prioritätsprüfung,
- Indizes,
- Versionsspalte für parallele Änderungen.

Die Anwendung verwendet:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Dadurch darf Hibernate das Schema nicht unkontrolliert verändern. Flyway ist die einzige Stelle für Schemaänderungen.

---

## 13. Datenbankkennwörter für Docker Compose anlegen

Nur für die lokale Docker-Compose-Demo:

```bash
cp .env.example .env
```

Zufällige Werte erzeugen:

```bash
DB_PASSWORD="$(openssl rand -hex 32)"
DB_ROOT_PASSWORD="$(openssl rand -hex 40)"

sed -i \
  "s|CHANGE_ME_WITH_A_LONG_RANDOM_VALUE|${DB_PASSWORD}|" \
  .env

sed -i \
  "s|CHANGE_ME_WITH_ANOTHER_LONG_RANDOM_VALUE|${DB_ROOT_PASSWORD}|" \
  .env
```

Prüfen, ohne die Kennwörter im Unterricht anzuzeigen:

```bash
grep -E '^(DB_NAME|DB_USERNAME|APP_PORT)=' .env
```

Die Datei `.env` ist durch `.gitignore` ausgeschlossen und darf nicht committed werden.

---

## 14. Datenbankkennwörter für Kubernetes

Für das lokale Deployment erstellt dieses Skript das Secret automatisch:

```bash
K8S_NAMESPACE=training ./scripts/ensure-database-secret.sh
```

Das Secret heißt:

```text
task-api-database
```

Prüfen:

```bash
kubectl get secret task-api-database -n training
```

Die Werte nicht mit `-o yaml` während der Präsentation anzeigen.

Für GitHub Actions können optional Repository-Secrets gesetzt werden:

```text
GitHub Repository
→ Settings
→ Secrets and variables
→ Actions
```

Namen:

```text
TRAINING_DB_PASSWORD
TRAINING_DB_ROOT_PASSWORD
```

Sind sie nicht gesetzt, erzeugt das Skript beim ersten Lauf sichere Zufallswerte.

Wichtig: Ein bestehendes Kubernetes Secret wird nicht automatisch überschrieben.

Kennwort bewusst neu erzeugen:

```bash
kubectl delete secret task-api-database -n training
./scripts/ensure-database-secret.sh
kubectl rollout restart statefulset/mariadb -n training
kubectl rollout restart deployment/task-api -n training
```

Bei einer bereits initialisierten MariaDB-Datenbank darf das Kennwort nicht unbedacht geändert werden. Das persistente Volume enthält weiterhin den alten Datenbankbenutzer. Für die Schulungsumgebung ist ein kompletter Reset oft einfacher.

---

# Teil D – Lokaler Test mit Docker Compose

## 15. Anwendung bauen und testen

```bash
./gradlew clean test bootJar
```

Dabei laufen:

- Unit-Tests,
- Controller-Tests,
- Integrationstest mit MariaDB/Testcontainers,
- Erstellung der JAR-Datei.

Ergebnis:

```text
build/libs/*.jar
```

---

## 16. Docker Compose starten

```bash
docker compose up --build -d
```

Status prüfen:

```bash
docker compose ps
```

Logs:

```bash
docker compose logs -f task-api
```

Health Check:

```bash
curl http://localhost:8080/api/health
```

Tasks abrufen:

```bash
curl http://localhost:8080/api/tasks
```

---

## 17. Demo-Daten einfügen

Die Datenbank muss nicht vorab befüllt werden. Für die Präsentation können Demo-Daten über die REST-Schnittstelle erzeugt werden:

```bash
BASE_URL=http://localhost:8080 ./scripts/seed-demo-data.sh
```

Danach:

```bash
curl http://localhost:8080/api/tasks | jq
```

Dieser Weg ist besser als manuelles SQL, weil gleichzeitig gezeigt wird:

- Schnittstelle,
- Validierung,
- Service-Schicht,
- Persistenz,
- MariaDB-Anbindung.

---

## 18. Optional direkt in MariaDB prüfen

Container ermitteln:

```bash
docker compose ps
```

Datenbankabfrage ausführen:

```bash
docker compose exec mariadb \
  mariadb \
  -u"${DB_USERNAME:-taskapp}" \
  -p \
  taskdb
```

In der MariaDB-Shell:

```sql
SHOW TABLES;
SELECT id, title, priority, completed FROM tasks;
EXIT;
```

Das Kennwort stammt aus `.env`.

Docker Compose beenden:

```bash
docker compose down
```

Daten behalten:

```bash
docker compose down
```

Daten vollständig löschen:

```bash
docker compose down --volumes
```

---

# Teil E – OpenTofu und Kubernetes vorbereiten

## 19. OpenTofu initialisieren und planen

```bash
./scripts/tofu.sh plan
```

OpenTofu verwaltet in dieser Demo den Namespace:

```text
training
```

Anwenden:

```bash
./scripts/tofu.sh apply
```

Prüfen:

```bash
kubectl get namespace training
```

Der State liegt dauerhaft unter:

```text
~/.local/state/task-api-training/terraform.tfstate
```

Nicht löschen, solange OpenTofu den Namespace verwalten soll.

---

## 20. Vollständiges Deployment nach Minikube

```bash
./scripts/deploy-local.sh
```

Das Skript führt aus:

```text
1. Umgebung prüfen
2. Tests und JAR-Build
3. OpenTofu Namespace anwenden
4. Datenbank-Secret erstellen
5. ConfigMap anwenden
6. MariaDB Service und StatefulSet deployen
7. Auf MariaDB warten
8. Task-API-Image in Minikube bauen
9. API Service und Deployment anwenden
10. Auf Rollout warten
11. Smoke-Test ausführen
```

Status prüfen:

```bash
kubectl get statefulset,deployment,pods,services,pvc -n training -o wide
```

Erwartung:

```text
MariaDB: 1 Pod Running und Ready
Task API: 2 Pods Running und Ready
PVC: Bound
```

---

## 21. Anwendung über Minikube aufrufen

```bash
MINIKUBE_IP="$(minikube ip -p minikube)"
BASE_URL="http://${MINIKUBE_IP}:30081"
```

Health Check:

```bash
curl "${BASE_URL}/api/health" | jq
```

Tasks:

```bash
curl "${BASE_URL}/api/tasks" | jq
```

Demo-Daten einfügen:

```bash
BASE_URL="${BASE_URL}" ./scripts/seed-demo-data.sh
```

---

## 22. Persistenz demonstrieren

Vorhandene Daten prüfen:

```bash
curl "${BASE_URL}/api/tasks" | jq
```

Nur API-Pods neu starten:

```bash
kubectl rollout restart deployment/task-api -n training
kubectl rollout status deployment/task-api -n training
```

Daten erneut abrufen:

```bash
curl "${BASE_URL}/api/tasks" | jq
```

Die Daten bleiben erhalten, weil sie in MariaDB und im PersistentVolume gespeichert sind.

PVC prüfen:

```bash
kubectl get pvc -n training
```

---

# Teil F – GitHub Actions vorbereiten

## 23. Workflow-Werte prüfen

Datei:

```text
.github/workflows/ci-cd.yml
```

Prüfen:

```yaml
env:
  JAVA_VERSION: "21"
  MINIKUBE_PROFILE: "minikube"
  K8S_NAMESPACE: "training"
  DEPLOYMENT_NAME: "task-api"
  CONTAINER_NAME: "task-api"
  IMAGE_NAME: "task-api"
```

Runner:

```yaml
runs-on: [self-hosted, linux, x64]
```

---

## 24. Repository nach GitHub pushen

```bash
git status
git add .
git commit -m "feat: prepare complete task api training environment"
git branch -M main
git push -u origin main
```

Danach in GitHub:

```text
Actions
→ Task API CI/CD
```

Die Pipeline besteht aus:

```text
verify
→ GitHub-gehosteter Runner
→ Build und Tests

deploy
→ Self-hosted Runner
→ Ansible-Prüfung
→ OpenTofu
→ MariaDB
→ Docker Image
→ Kubernetes Rollout
→ Smoke-Test
```

---

# Teil G – Häufige Anpassungen und Fehler

## 25. Minikube läuft nach VM-Neustart nicht

```bash
minikube start -p minikube --driver docker
```

Danach:

```bash
kubectl get nodes
```

---

## 26. Runner ist offline

```bash
cd ~/Dokumente/actions-runner
sudo ./svc.sh status
sudo ./svc.sh start
```

---

## 27. MariaDB-Pod startet nicht

```bash
kubectl get pods -n training
kubectl describe pod -n training -l app.kubernetes.io/name=mariadb
kubectl logs -n training statefulset/mariadb
```

Secret prüfen:

```bash
kubectl get secret task-api-database -n training
```

PVC prüfen:

```bash
kubectl get pvc -n training
```

---

## 28. Task-API kann MariaDB nicht erreichen

```bash
kubectl get service mariadb -n training
kubectl get configmap task-api-config -n training -o yaml
kubectl logs -n training deployment/task-api
```

Erwartete URL:

```text
jdbc:mariadb://mariadb:3306/taskdb
```

`mariadb` ist der interne Kubernetes-Service-Name.

---

## 29. Image wird nicht gefunden

```bash
minikube image ls -p minikube | grep task-api
```

Deployment-Image prüfen:

```bash
kubectl get deployment task-api -n training \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

---

## 30. Komplett zurücksetzen

Nur Kubernetes-Anwendungsressourcen entfernen, Daten behalten:

```bash
./scripts/cleanup.sh
```

Daten und PVC ebenfalls löschen:

```bash
DELETE_DATA=true ./scripts/cleanup.sh
```

Komplettes Minikube löschen:

```bash
minikube delete -p minikube
```

Danach neu starten:

```bash
minikube start \
  -p minikube \
  --driver docker \
  --cpus 4 \
  --memory 6144
```

---

# Teil H – Empfohlene Reihenfolge für deine Vorbereitung

```text
1. VM kopieren und Hostname ändern
2. Runner auf der Kopie neu registrieren
3. Docker und Minikube prüfen
4. Repository außerhalb von _work entpacken
5. Skriptrechte setzen
6. Repository-Werte prüfen
7. Ansible verify ausführen
8. Gradle-Tests ausführen
9. Docker-Compose-Demo testen
10. OpenTofu plan und apply ausführen
11. Minikube-Deployment ausführen
12. Demo-Daten über die API anlegen
13. Persistenz testen
14. Repository nach GitHub pushen
15. GitHub-Actions-Pipeline vorführen
```

---

# Kurzfassung für die Vorstellung

```text
Ansible prüft die VM.
OpenTofu verwaltet den Kubernetes-Namespace.
Gradle baut und testet die Task-API.
Docker erstellt das Anwendungs-Image.
MariaDB läuft als separates Image.
Minikube stellt das lokale Kubernetes-Cluster bereit.
Kubernetes betreibt Datenbank, Pods, Services und PersistentVolume.
GitHub Actions automatisiert den gesamten Ablauf.
```
