# Task-API

Basis-URL lokal über Minikube:

```text
http://MINIKUBE-IP:30081
```

## Endpunkte

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/api/health` | Anwendung und Datenbank prüfen |
| GET | `/api/tasks` | alle Tasks lesen |
| GET | `/api/tasks/{id}` | einzelnen Task lesen |
| POST | `/api/tasks` | Task anlegen |
| PUT | `/api/tasks/{id}` | Task vollständig ändern |
| PATCH | `/api/tasks/{id}/completion` | Erledigt-Status ändern |
| DELETE | `/api/tasks/{id}` | Task löschen |

## Beispiel: Task anlegen

```bash
curl -X POST http://MINIKUBE-IP:30081/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "CI/CD erklären",
    "description": "Pipeline mit MariaDB demonstrieren",
    "priority": "HIGH",
    "dueDate": "2030-12-31"
  }'
```

Ungültige Eingaben werden als standardisierte `application/problem+json`-Antwort geliefert.
