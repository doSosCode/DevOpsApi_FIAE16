# Architektur- und Sicherheitsentscheidungen

## Ziel

Das Projekt ist eine verständliche Schulungsanwendung, soll aber zentrale Praktiken aus professionellen Projekten zeigen. Es ist keine vollständige Produktionsplattform.

## Schichten

```text
HTTP/API
  -> TaskService (Anwendungsfälle)
  -> TaskRepository (fachlicher Port)
  -> JpaTaskStore (technischer Adapter)
  -> Spring Data JPA / MariaDB
```

Die Anwendungsschicht hängt nicht direkt von JPA ab. Dadurch kann die Persistenz später ausgetauscht oder im Unit-Test gemockt werden.

## Kommentare

Kommentare erklären nur Entscheidungen, die nicht direkt aus dem Code hervorgehen, zum Beispiel:

- warum Ergebnislisten begrenzt werden,
- warum numerische Container-IDs verwendet werden,
- warum PR-Code nicht auf dem Self-hosted Runner läuft,
- warum OpenTofu-State außerhalb des Runner-Arbeitsverzeichnisses liegt.

Offensichtliche Zeilen werden nicht kommentiert, damit Kommentare nicht veralten und den Code überladen.

## API-Sicherheit ohne Authentifizierung

Aktuell ist die API absichtlich ohne Anmeldung erreichbar. Trotzdem umgesetzt:

- Bean Validation für Nutzdaten und Parameter,
- maximale Ergebnisgröße von 100 Tasks,
- begrenzte HTTP-Header- und Request-Größen,
- keine Stacktraces oder internen Fehlermeldungen im Client,
- generische Antwort bei unerwarteten Fehlern,
- defensive HTTP-Header,
- Request-ID zur Zuordnung von Anfragen,
- Optimistic Locking und HTTP 409 bei parallelen Änderungen,
- Datenbankzugang nur über Kubernetes Secret.

Die geplante Authentifizierung ist in `AUTHENTICATION_EXTENSION.md` beschrieben.

## CI/CD-Sicherheit

Pull-Request-Code läuft auf `ubuntu-latest`, nicht auf dem Self-hosted Runner. Der Self-hosted Runner hat Zugriff auf Docker, Minikube und lokale Infrastruktur und wird deshalb nur für vertrauenswürdige Commits auf `main` verwendet.

Actions erhalten nur `contents: read`. Beim Checkout werden Git-Zugangsdaten nicht im Arbeitsverzeichnis gespeichert.

## Container und Kubernetes

Task-API:

- numerischer Non-Root-Benutzer,
- Read-only Root-Dateisystem,
- alle Linux Capabilities entfernt,
- kein Service-Account-Token,
- Seccomp RuntimeDefault,
- begrenztes `/tmp`,
- Ressourcenanforderungen und Limits,
- Startup-, Readiness- und Liveness-Probes,
- Graceful Shutdown und PreStop-Verzögerung.

MariaDB:

- persistenter Datenträger,
- kein Service-Account-Token,
- Seccomp RuntimeDefault,
- nur benötigte Linux Capabilities,
- Startup-, Readiness- und Liveness-Probes,
- Ressourcenlimits.

Die optionale NetworkPolicy wird nicht automatisch angewendet, da nicht jedes Minikube-CNI NetworkPolicies durchsetzt.

## OpenTofu-State

Der State liegt standardmäßig unter:

```text
~/.local/state/task-api-training/terraform.tfstate
```

Damit bleibt er über GitHub-Actions-Checkouts hinweg erhalten. Im Repository und im Runner-Verzeichnis wird kein State committed oder dauerhaft gespeichert.

## Bewusste Grenzen

Für eine echte Produktionsumgebung fehlen unter anderem:

- HTTPS/TLS und Ingress,
- Authentifizierung und Autorisierung,
- externes Secret-Management,
- verschlüsselte Datenträger und Backups,
- Image-Signierung und SBOM,
- zentrale Logs, Metriken und Traces,
- hochverfügbare Datenbank,
- getrennte Cluster für Staging und Produktion,
- vollständig nach Commit-SHA gepinnte GitHub Actions und Container-Digests.
