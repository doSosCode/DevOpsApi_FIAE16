#!/usr/bin/env bash
set -euo pipefail

# Richtet den persönlichen SSH-Schluessel fuer manuelle GitHub-Zugriffe ein.
# Der GitHub Actions Runner selbst benoetigt diesen Schluessel nicht: actions/checkout
# verwendet waehrend eines Jobs ein kurzlebiges GitHub-Token.

GIT_EMAIL="${1:-${GIT_EMAIL:-}}"
KEY_PATH="${HOME}/.ssh/id_ed25519"

if [[ -z "${GIT_EMAIL}" ]]; then
  echo "Aufruf: $0 DEINE_GITHUB_EMAIL" >&2
  echo "Alternativ: GIT_EMAIL=... $0" >&2
  exit 2
fi

install -d -m 0700 "${HOME}/.ssh"

if [[ -f "${KEY_PATH}" ]]; then
  echo "Vorhandener SSH-Schluessel wird nicht ueberschrieben: ${KEY_PATH}"
else
  ssh-keygen \
    -t ed25519 \
    -a 100 \
    -C "${GIT_EMAIL}" \
    -f "${KEY_PATH}"
fi

chmod 0600 "${KEY_PATH}"
chmod 0644 "${KEY_PATH}.pub"

cat <<NOTICE

Der oeffentliche Schluessel lautet:

NOTICE
cat "${KEY_PATH}.pub"
cat <<'NOTICE'

Naechste manuelle Schritte:
1. GitHub -> Settings -> SSH and GPG keys -> New SSH key
2. Den oben angezeigten oeffentlichen Schluessel eintragen.
3. Verbindung testen: ssh -T git@github.com
4. Beim ersten Verbindungsaufbau den Host-Fingerprint mit der offiziellen
   GitHub-Dokumentation vergleichen, bevor er bestaetigt wird.

Der private Schluessel ~/.ssh/id_ed25519 darf niemals committed oder geteilt werden.
NOTICE
