# Dozenten-Anleitung: Vom leeren Ubuntu bis zur fertigen CI/CD-Demo

Diese Anleitung trennt bewusst zwischen einmaligen manuellen Schritten und den
Aufgaben, die danach durch GitHub Actions, Ansible, OpenTofu, Docker und
Kubernetes automatisiert werden.

## 1. Zielbild

Am Ende gilt:

```text
Hyper-V
`-- Ubuntu-VM
    |-- GitHub Self-hosted Runner
    |-- Ansible
    |-- Java 21
    |-- Docker
    |-- kubectl
    |-- Minikube
    |-- OpenTofu
    `-- Minikube/Kubernetes
        |-- MariaDB StatefulSet + PersistentVolumeClaim
        |-- Task-API Deployment mit zwei Pods
        |-- NodePort-Service fuer HTTP
        `-- NGINX Ingress + lokales TLS-Zertifikat
```

## 2. Was muss zwingend manuell geschehen?

Eine nahezu leere VM kann erst durch GitHub Actions konfiguriert werden, wenn
auf ihr bereits ein registrierter Self-hosted Runner laeuft. Deshalb bleiben
folgende Schritte manuell:

1. Ubuntu-VM anlegen und installieren.
2. Netzwerk und SSH pruefen.
3. Minimale Pakete fuer Runner und Download installieren.
4. Repository einmalig nach GitHub hochladen.
5. Self-hosted Runner auf der VM registrieren.
6. Bootstrap-Workflow starten.
7. VM einmal neu starten.
8. Persoenlichen SSH-Zugang fuer eigene Git-Pushes einrichten.
9. Deployment-Workflow starten.

Alles ab der Host-Softwareinstallation wird danach reproduzierbar automatisiert.

---

# Phase A: Repository erstmalig zu GitHub bringen

## 3. Leeres GitHub-Repository anlegen

In GitHub:

```text
New repository
-> Repository name festlegen
-> Private oder Public auswaehlen
-> README, .gitignore und License nicht automatisch erzeugen
-> Create repository
```

Das Repository muss leer sein, damit das vorhandene Projekt ohne Konflikt
hochgeladen werden kann.

## 4. Git auf dem Rechner fuer den ersten Push konfigurieren

Der erste Push muss erfolgen, bevor der Bootstrap-Workflow existiert. Deshalb
wird Git auf dem Rechner konfiguriert, auf dem die ZIP-Datei entpackt wurde.
Das kann der Windows-Host oder bereits die Ubuntu-VM sein.

```bash
git config --global user.name "DEIN NAME"
git config --global user.email "DEINE_GITHUB_EMAIL_ODER_NOREPLY_ADRESSE"
git config --global init.defaultBranch main
```

Pruefen:

```bash
git config --global --get user.name
git config --global --get user.email
```

## 5. Projekt initialisieren und pushen

Im entpackten Projektordner:

```bash
git init
git branch -M main
git add .
git status
git commit -m "feat: initialize task api training platform"
```

Remote setzen. Fuer SSH:

```bash
git remote add origin git@github.com:DEIN-BENUTZER/DEIN-REPOSITORY.git
git push -u origin main
```

Fuer HTTPS kann alternativ die von GitHub angezeigte HTTPS-Adresse verwendet
werden. GitHub akzeptiert fuer Git ueber HTTPS kein Kontopasswort; erforderlich
ist dann ein geeigneter Credential Manager oder ein Personal Access Token.
Fuer die dauerhafte Arbeit auf der Ubuntu-VM wird in dieser Anleitung SSH
verwendet.

Pruefen:

```bash
git remote -v
git branch -vv
```

---

# Phase B: Ubuntu-VM unter Hyper-V vorbereiten

## 6. Hyper-V-VM anlegen

Empfehlung:

```text
Ubuntu Server 24.04 LTS
Generation 2
4 vCPU
8 GB RAM
mindestens 40 GB dynamische Festplatte
externer Hyper-V-Switch oder ein anderes nachvollziehbares VM-Netz
```

Bei der Ubuntu-Installation:

- Benutzer `devops` anlegen,
- OpenSSH Server aktivieren,
- keine MariaDB direkt auf Ubuntu installieren.

## 7. Netzwerk und System pruefen

Auf der VM anmelden:

```bash
hostnamectl
ip address
ip route
ping -c 3 github.com
```

Von einem anderen Rechner kann SSH getestet werden:

```bash
ssh devops@IP_DER_VM
```

## 8. Minimale Pakete installieren

Nur die Werkzeuge installieren, die fuer Runner-Download und Bootstrap
zwingend erforderlich sind:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git openssh-client
```

Java, Docker, kubectl, Minikube, OpenTofu und Ansible werden spaeter durch den
Bootstrap-Ablauf installiert.

## 9. Nicht-interaktives sudo fuer die isolierte Demo-VM

GitHub Actions kann kein sudo-Passwort interaktiv eingeben. Fuer diese isolierte
Schulungs-VM:

```bash
echo "devops ALL=(ALL) NOPASSWD: ALL" \
  | sudo tee /etc/sudoers.d/90-devops-training

sudo chmod 0440 /etc/sudoers.d/90-devops-training
sudo visudo -cf /etc/sudoers.d/90-devops-training
```

Erwartung:

```text
/etc/sudoers.d/90-devops-training: parsed OK
```

Sicherheitshinweis: Diese weitreichende Regel ist nur fuer eine isolierte,
wegwerfbare Demo-VM gedacht. Ein produktiver Runner benoetigt restriktivere
Berechtigungen und sollte nicht fuer ungeprueften Code verwendet werden.

---

# Phase C: Self-hosted Runner registrieren

## 10. Runner in GitHub anlegen

Im Repository:

```text
Settings
-> Actions
-> Runners
-> New self-hosted runner
-> Linux
-> x64
```

Die von GitHub angezeigten Befehle exakt auf der VM ausfuehren. GitHub erzeugt
dafuer ein kurzlebiges Registrierungstoken. Dieses Token nicht dokumentieren
oder committen.

Empfohlener Installationsort:

```text
/home/devops/actions-runner
```

Nach der interaktiven Konfiguration:

```bash
cd ~/actions-runner
sudo ./svc.sh install devops
sudo ./svc.sh start
sudo ./svc.sh status
```

In GitHub muss der Runner als `Idle` erscheinen.

Hinweis: Der Runner kann das Repository jetzt bereits mit `actions/checkout`
auschecken. Dafuer ist noch kein persönlicher SSH-Schluessel auf der VM noetig.

---

# Phase D: Bootstrap-Workflow ausfuehren

## 11. Workflow starten

In GitHub:

```text
Actions
-> 01 - Bootstrap Ubuntu VM
-> Run workflow
```

Eingaben:

```text
Git-Anzeigename:
DEIN NAME

Git-E-Mail:
bei GitHub hinterlegte E-Mail oder GitHub-No-Reply-Adresse
```

Der Workflow macht Folgendes:

```text
actions/checkout
-> minimales Ansible per Shell installieren
-> ansible/prepare-host.yml ausfuehren
-> Git global fuer den Runner-Benutzer konfigurieren
-> Java 21 installieren
-> Docker installieren und aktivieren
-> kubectl installieren
-> Minikube installieren
-> OpenTofu installieren
-> Verzeichnisse und Rechte anlegen
-> Installation pruefen
```

## 12. Bootstrap-Ergebnis pruefen

Nach erfolgreichem Workflow auf der VM:

```bash
git config --global --list
java -version
docker --version
kubectl version --client
minikube version
tofu version
ansible-playbook --version
```

Docker kann im noch laufenden Runner-Prozess trotzdem vor dem Neustart eine
Berechtigungsfehlermeldung liefern. Das ist erwartbar.

## 13. VM neu starten

```bash
sudo reboot
```

Danach warten, bis:

```text
GitHub -> Settings -> Actions -> Runners -> Status: Idle
```

Nun pruefen:

```bash
docker info
```

Der Befehl muss ohne `sudo` funktionieren.

---

# Phase E: Persoenlichen Git-Zugang auf der VM einrichten

## 14. Eigenes Arbeitsverzeichnis verwenden

Nicht im Runner-Arbeitsverzeichnis entwickeln:

```text
~/actions-runner/_work
```

Stattdessen:

```bash
mkdir -p ~/Projekte
cd ~/Projekte
```

## 15. SSH-Schluessel erzeugen

Da das Projekt fuer das Skript zunaechst geklont werden muss, gibt es zwei
Moeglichkeiten:

### Moeglichkeit A: Repository einmal per HTTPS klonen

```bash
git clone HTTPS_REPOSITORY_ADRESSE task-api-training
cd task-api-training
./scripts/setup-git-ssh.sh DEINE_GITHUB_EMAIL
```

Danach Remote auf SSH umstellen:

```bash
git remote set-url origin \
  git@github.com:DEIN-BENUTZER/DEIN-REPOSITORY.git
```

### Moeglichkeit B: SSH-Schluessel ohne Skript erzeugen

```bash
install -d -m 0700 ~/.ssh
ssh-keygen -t ed25519 -a 100 -C "DEINE_GITHUB_EMAIL"
cat ~/.ssh/id_ed25519.pub
```

Den oeffentlichen Schluessel in GitHub eintragen:

```text
GitHub-Konto
-> Settings
-> SSH and GPG keys
-> New SSH key
```

Verbindung testen:

```bash
ssh -T git@github.com
```

Beim ersten Verbindungsaufbau den angezeigten Host-Fingerprint mit der
offiziellen GitHub-Dokumentation vergleichen, bevor er bestaetigt wird.

Danach klonen:

```bash
cd ~/Projekte
git clone git@github.com:DEIN-BENUTZER/DEIN-REPOSITORY.git task-api-training
cd task-api-training
```

## 16. Git-Zustand pruefen

```bash
git config --global --get user.name
git config --global --get user.email
git remote -v
git status
git branch -vv
```

---

# Phase F: Optionale GitHub-Secrets

## 17. Datenbankkennwoerter hinterlegen

Im Repository:

```text
Settings
-> Secrets and variables
-> Actions
-> New repository secret
```

Optional:

```text
TRAINING_DB_PASSWORD
TRAINING_DB_ROOT_PASSWORD
```

Empfohlene zufaellige Werte auf der VM erzeugen:

```bash
openssl rand -hex 32
openssl rand -hex 40
```

Ohne diese Secrets erzeugt `scripts/ensure-database-secret.sh` beim ersten
Deployment zufaellige Kennwoerter und speichert sie als Kubernetes Secret.
Vorhandene Secrets werden bei spaeteren Deployments nicht unbemerkt ersetzt.

---

# Phase G: Build- und Deployment-Workflow

## 18. Workflow starten

Der Workflow startet automatisch bei einem Push nach `main` oder manuell:

```text
Actions
-> 02 - Build, Test and Deploy
-> Run workflow
```

## 19. Verify-Job

Der erste Job laeuft auf einem GitHub-gehosteten Runner:

```text
Repository auschecken
-> Java 21 einrichten
-> Gradle-Abhaengigkeiten cachen
-> Unit-, Controller- und Integrationstests ausfuehren
-> bootJar bauen
```

Die MariaDB-Integrationstests verwenden Testcontainers und starten eine
kurzlebige MariaDB nur fuer den Test.

Pull Requests fuehren nur diesen sicheren Pruefteil aus.

## 20. Deploy-Job

Nur nach erfolgreichem Verify-Job und nicht bei Pull Requests:

```text
Self-hosted Runner
-> Repository auschecken
-> Ansible prueft Host und startet Minikube
-> Ansible aktiviert NGINX Ingress
-> Gradle erzeugt das JAR
-> OpenTofu verwaltet Namespace training
-> Kubernetes Secret fuer MariaDB sicherstellen
-> MariaDB Service und StatefulSet deployen
-> auf MariaDB-Bereitschaft warten
-> Docker-Image task-api:<Commit-SHA> direkt in Minikube bauen
-> Task-API Service, PDB und Deployment anwenden
-> lokale CA und Serverzertifikat erzeugen
-> Kubernetes TLS Secret und Ingress anwenden
-> auf Rolling Update warten
-> HTTP- und HTTPS-Smoke-Test
```

## 21. Was OpenTofu hier macht

OpenTofu verbindet sich ueber `~/.kube/config` mit Minikube und verwaltet den
Namespace:

```text
training
```

Der State wird dauerhaft ausserhalb des Runner-Workspaces gespeichert:

```text
~/.local/state/task-api-training/terraform.tfstate
```

Damit erkennt OpenTofu beim naechsten Lauf, dass der Namespace bereits
verwaltet wird.

## 22. Was Ansible hier macht

Bootstrap:

```text
Host-Pakete und Werkzeuge installieren
Git konfigurieren
Docker-Dienst und Benutzerrechte konfigurieren
```

Normaler Deployment-Lauf:

```text
Werkzeuge pruefen
Minikube bei Bedarf starten
Ingress-Addon aktivieren
auf Node und Ingress-Controller warten
```

## 23. Was Docker und Kubernetes machen

Docker baut:

```text
task-api:<12 Zeichen des Git-Commit-SHA>
```

Kubernetes betreibt:

```text
mariadb:11.4 als StatefulSet
Task-API als Deployment mit zwei Pods
Services, PersistentVolumeClaim, Secret und Ingress
```

---

# Phase H: Ergebnis kontrollieren

## 24. Kubernetes-Ressourcen pruefen

```bash
kubectl get statefulset,deployment,pods,service,ingress,pvc \
  --namespace training \
  --output wide
```

Erwartung:

```text
mariadb-0                  Running / Ready
task-api-...               Running / Ready
task-api-...               Running / Ready
data-mariadb-0             Bound
Service mariadb            ClusterIP
Service task-api           NodePort 30081
Ingress task-api           vorhanden
```

## 25. Logs pruefen

```bash
kubectl logs -n training statefulset/mariadb --tail=100
kubectl logs -n training deployment/task-api --tail=100
```

## 26. HTTP testen

Auf der Ubuntu-VM:

```bash
BASE_URL="http://$(minikube ip -p minikube):30081"
curl "${BASE_URL}/api/health" | jq
curl "${BASE_URL}/api/tasks" | jq
```

## 27. HTTPS testen

Ohne hosts-Datei:

```bash
curl \
  --cacert ~/.local/share/task-api-training/tls/training-ca.crt \
  --resolve "task-api.local:443:$(minikube ip -p minikube)" \
  https://task-api.local/api/health
```

Dabei werden CA, Hostname und Zertifikat wirklich geprueft. `curl -k` wird
bewusst nicht verwendet.

---

# Phase I: Datenbank und Demo-Daten

## 28. Datenbankinitialisierung verstehen

Keine MariaDB-Installation per `apt` ist erforderlich.

```text
Kubernetes startet mariadb:11.4
-> MariaDB-Image liest Werte aus dem Kubernetes Secret
-> Datenbank taskdb und Benutzer taskapp werden erzeugt
-> Task-API verbindet sich mit mariadb:3306
-> Flyway fuehrt V1__create_tasks_table.sql aus
-> Tabelle tasks wird erzeugt
```

## 29. Demo-Daten ueber die API eintragen

```bash
cd ~/Projekte/task-api-training
BASE_URL="http://$(minikube ip -p minikube):30081" \
  ./scripts/seed-demo-data.sh
```

Pruefen:

```bash
curl "http://$(minikube ip -p minikube):30081/api/tasks" | jq
```

Die Befuellung ueber die REST-API zeigt den kompletten Datenweg:

```text
HTTP -> Controller -> Service -> Repository -> JPA -> MariaDB
```

## 30. Datenbank direkt inspizieren

Benutzer und Datenbankname aus dem Secret lesen:

```bash
DB_USER="$(kubectl get secret task-api-database -n training \
  -o jsonpath='{.data.database-user}' | base64 --decode)"
DB_NAME="$(kubectl get secret task-api-database -n training \
  -o jsonpath='{.data.database-name}' | base64 --decode)"
```

Interaktive Anmeldung:

```bash
kubectl exec -it -n training mariadb-0 -- \
  mariadb -u "${DB_USER}" -p "${DB_NAME}"
```

In MariaDB:

```sql
SHOW TABLES;
DESCRIBE tasks;
SELECT * FROM tasks;
exit;
```

Das Passwort nur fuer die lokale Demo bei Bedarf anzeigen:

```bash
kubectl get secret task-api-database -n training \
  -o jsonpath='{.data.database-password}' | base64 --decode
echo
```

---

# Phase J: Browserzugriff und Zertifikate

## 31. Zugriff von Windows ueber HTTP

Die Minikube-IP beim Docker-Treiber ist vom Windows-Host haeufig nicht direkt
routebar. Fuer die Demonstration deshalb auf der Ubuntu-VM:

```bash
cd ~/Projekte/task-api-training
./scripts/start-port-forward.sh
```

Standard:

```text
VM-Port 8081 -> Kubernetes Service task-api:8080
```

Falls UFW aktiv ist:

```bash
sudo ufw allow 8081/tcp
```

Auf Windows:

```text
http://IP_DER_UBUNTU_VM:8081/api/health
```

Der Prozess endet mit dem Terminal oder `Ctrl+C`.

## 32. Lokale CA fuer HTTPS exportieren

```bash
./scripts/export-training-ca.sh
```

Die erzeugte CA-Datei auf den Windows-Host kopieren und nur in der isolierten
Schulungsumgebung als vertrauenswuerdig importieren. Details stehen in:

```text
docs/ZERTIFIKATE_UND_HTTPS.md
```

Der automatische HTTPS-Smoke-Test der Pipeline laeuft innerhalb der VM direkt
gegen die Minikube-IP. Ein Browserzugriff vom Windows-Host kann je nach
Hyper-V- und Docker-Netz zusaetzliches Routing oder einen separaten HTTPS-
Port-Forward erfordern.

---

# Phase K: Versionierung und Rolling Update demonstrieren

## 33. Feature-Branch erstellen

```bash
cd ~/Projekte/task-api-training
git switch -c feature/demo-change
```

Code aendern und pruefen:

```bash
git status
git diff
```

Committen und pushen:

```bash
git add .
git commit -m "feat: demonstrate rolling update"
git push -u origin feature/demo-change
```

Pull Request erstellen. Der Verify-Job testet, deployt aber nicht.

Nach Review nach `main` mergen. Dadurch startet der Deployment-Job.

## 34. Rolling Update beobachten

Auf der VM:

```bash
kubectl get pods -n training -w
```

Kubernetes startet neue Task-API-Pods mit dem neuen Commit-Image und beendet
die alten Pods erst nach erfolgreicher Readiness. MariaDB und PVC bleiben
bestehen.

Image kontrollieren:

```bash
kubectl get deployment task-api -n training \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

---

# Phase L: Wiederholbarkeit und Aufraeumen

## 35. Workflows erneut ausfuehren

- Der Bootstrap-Workflow ist idempotent und kann erneut ausgefuehrt werden.
- OpenTofu erkennt den bereits verwalteten Namespace.
- Kubernetes aktualisiert vorhandene Ressourcen deklarativ.
- MariaDB-Daten bleiben im PVC erhalten.
- Das API-Image erhaelt bei jedem Commit einen neuen unveraenderlichen Tag.

## 36. Anwendung entfernen

```bash
cd ~/Projekte/task-api-training
./scripts/cleanup.sh
```

## 37. Minikube vollstaendig entfernen

```bash
minikube delete -p minikube
```

## 38. Lokale Zertifikate entfernen

```bash
rm -rf ~/.local/share/task-api-training/tls
```

Eine auf Windows importierte Demo-CA muss dort ebenfalls wieder aus dem
Zertifikatsspeicher entfernt werden.

---

# Schnellcheck vor dem Unterricht

```bash
# Runner online?
sudo ~/actions-runner/svc.sh status

# Git korrekt?
git config --global --get user.name
git config --global --get user.email

# Docker-Zugriff?
docker info >/dev/null && echo OK

# Cluster bereit?
minikube status -p minikube
kubectl get nodes

# Anwendung bereit?
kubectl get pods -n training
curl "http://$(minikube ip -p minikube):30081/api/health"

# Zertifikat vorhanden?
test -f ~/.local/share/task-api-training/tls/training-ca.crt && echo OK
```
