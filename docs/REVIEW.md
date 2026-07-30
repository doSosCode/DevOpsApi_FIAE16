# Durchgeführte Qualitätsprüfung

## Behobene hohe Risiken

1. **Unvertrauenswürdiger PR-Code auf Self-hosted Runner**  
   CI für Pull Requests läuft jetzt auf einem isolierten GitHub-Runner. Deployment nutzt den Self-hosted Runner nur für `main` und manuelle Starts.

2. **Verlorener OpenTofu-State im Runner-Arbeitsverzeichnis**  
   Der State wird dauerhaft außerhalb von `_work` gespeichert.

3. **Fest eingetragene Docker-Compose-Kennwörter**  
   Kennwörter müssen über eine nicht versionierte `.env` gesetzt werden.

4. **Direkte Abhängigkeit der Anwendungslogik von JPA**  
   `TaskRepository` dient jetzt als fachlicher Port; `JpaTaskStore` ist der Adapter.

5. **Unbegrenztes Laden aller Tasks**  
   `GET /api/tasks` unterstützt `limit=1..100` und lädt standardmäßig höchstens 50 Einträge.

## Weitere Verbesserungen

- generische Fehlerbehandlung ohne Offenlegung interner Details,
- 409 bei Optimistic-Locking-Konflikten,
- Request-ID und zusätzliche Security Header,
- begrenzte HTTP-Anfragegrößen,
- stabiler Docker-Artefaktname `app.jar`,
- Startup-Probes und Graceful Shutdown,
- PodDisruptionBudget,
- deaktivierte Service-Account-Tokens,
- persistentes und wartbares Cleanup-Verhalten,
- Datenbank-Indizes und Check Constraint,
- zusätzliche Unit- und Web-Tests,
- Architekturdokumentation und gezielte Kommentare.
