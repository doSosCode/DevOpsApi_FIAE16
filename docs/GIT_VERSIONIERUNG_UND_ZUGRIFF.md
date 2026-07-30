# Git-Versionierung und GitHub-Zugriff

## 1. Zwei getrennte Git-Zugriffe

In diesem Projekt gibt es zwei unterschiedliche Zugriffsarten:

```text
GitHub Actions Runner
-> checkt Quellcode fuer einen einzelnen Workflow-Lauf aus
-> verwendet ein kurzlebiges GitHub-Token
-> erstellt normalerweise keine Commits

Dozent oder Entwickler
-> arbeitet in einem eigenen Projektordner
-> erstellt Branches, Commits und Pull Requests
-> verwendet fuer Pushes einen persoenlichen SSH-Schluessel
```

Der Runner-Arbeitsordner `~/actions-runner/_work` ist kein dauerhaftes
Entwicklungsverzeichnis.

## 2. Git-Konfiguration durch den Bootstrap-Workflow

Beim Start von `01 - Bootstrap Ubuntu VM` werden abgefragt:

- Git-Anzeigename,
- Git-E-Mail-Adresse.

Ansible erzeugt daraus fuer den Runner-Benutzer die Datei:

```text
~/.gitconfig
```

Konfiguriert werden:

```text
user.name
user.email
init.defaultBranch=main
pull.rebase=false
fetch.prune=true
core.autocrlf=input
core.editor=nano
push.default=simple
```

Die E-Mail-Adresse sollte bei GitHub hinterlegt sein. Alternativ kann die
GitHub-No-Reply-Adresse verwendet werden.

Pruefung:

```bash
git config --global --list
```

## 3. Persoenlichen SSH-Schluessel einrichten

Nach dem Bootstrap und dem Neustart auf der VM:

```bash
cd ~/Projekte/task-api-training
./scripts/setup-git-ssh.sh DEINE_GITHUB_EMAIL
```

Das Skript:

1. erstellt bei Bedarf `~/.ssh/id_ed25519`,
2. ueberschreibt keinen vorhandenen Schluessel,
3. zeigt nur den oeffentlichen Schluessel an,
4. speichert keinerlei Token im Repository.

Den ausgegebenen oeffentlichen Schluessel in GitHub hinterlegen:

```text
GitHub -> Settings -> SSH and GPG keys -> New SSH key
```

Verbindung testen:

```bash
ssh -T git@github.com
```

Beim ersten Verbindungsaufbau muss der angezeigte Host-Fingerprint mit der
offiziellen GitHub-Dokumentation verglichen werden. Erst danach bestaetigen.

Niemals weitergeben oder committen:

```text
~/.ssh/id_ed25519
```

## 4. Repository auf der VM klonen

```bash
mkdir -p ~/Projekte
cd ~/Projekte

git clone git@github.com:DEIN-BENUTZER/DEIN-REPOSITORY.git task-api-training
cd task-api-training
```

Pruefen:

```bash
git status
git remote -v
git branch -vv
```

## 5. Repository aus einer ZIP-Datei initialisieren

Dieser Schritt wird auf dem Rechner ausgefuehrt, von dem das Projekt erstmalig
zu GitHub hochgeladen wird. Das kann der Windows-Host oder die Ubuntu-VM sein.

```bash
cd PFAD_ZUM_ENTPACKTEN_PROJEKT

git init
git branch -M main
git add .
git commit -m "feat: initialize task api training platform"
git remote add origin git@github.com:DEIN-BENUTZER/DEIN-REPOSITORY.git
git push -u origin main
```

Wichtig: Fuer den ersten Push muessen Git-Identitaet und GitHub-Zugang auf
diesem Rechner bereits funktionieren. Der Bootstrap-Workflow kann erst laufen,
nachdem das Repository auf GitHub vorhanden und der Runner registriert ist.

## 6. Empfohlener Arbeitsablauf

```bash
git switch -c feature/demo-change
```

Aenderungen pruefen:

```bash
git status
git diff
```

Gezielt vormerken und committen:

```bash
git add src/main/java
git add src/test/java
git commit -m "feat: demonstrate a task api change"
```

Branch pushen:

```bash
git push -u origin feature/demo-change
```

Danach auf GitHub einen Pull Request erstellen. Der Pruefjob laeuft ohne
Deployment. Nach Review und Merge nach `main` startet der Deployment-Workflow.

## 7. Commit-Konvention

```text
feat:     neue Funktion
fix:      Fehlerbehebung
test:     Tests
docs:     Dokumentation
refactor: interne Verbesserung
ci:       GitHub-Actions-Aenderung
build:    Build-Konfiguration
chore:    Wartung
```

Beispiele:

```bash
git commit -m "feat: add task completion endpoint"
git commit -m "test: add MariaDB integration test"
git commit -m "ci: bootstrap Git configuration"
git commit -m "docs: explain local certificate setup"
```

## 8. Sicherheitsregeln

Nicht committen:

- `.env`,
- Datenbankkennwoerter,
- GitHub-Tokens,
- private SSH-Schluessel,
- private TLS-Schluessel,
- OpenTofu-State-Dateien.

`actions/checkout` verwendet `persist-credentials: false`. Dadurch verbleibt
das Workflow-Token nach dem Checkout nicht in der lokalen Git-Konfiguration.
