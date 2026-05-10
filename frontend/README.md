# CraftVoice - Angebotserstellung
Die CraftVoice-App ermöglicht eine schnelle und einfache Angebotserstellung im Handwerk. 
Nutzer erfassen dafür Leistungen, Materialien und Aufwände, während das System im Hintergrund automatisch ein professionelles Angebot generiert.

# Setup
### Software
Zum starten der Anwendung wird Docker benötigt.

### Step-by-Step Guide
Repo Klonen:
`git clone https://<Username>:<Token>@github.com/thi-projekte/handwerker.git`.
*Token können in den Einstellungen unter `https://github.com/settings/tokens` angelegt werden*.

# Wichtige Befehle/Skripte
Tbd

# Projektstruktur
Tbd


## CIB seven lokal starten

Für die lokale Entwicklung läuft CIB seven als Docker-Container.

**Voraussetzung:** Docker Desktop ist installiert und gestartet.

### Container starten

```bash
docker run -d --name cibseven -p 8080:8080 cibseven/cibseven:latest
```

### Weboberflächen

Nach dem Start erreichbar unter http://localhost:8080/webapp/

| Oberfläche | URL |
|---|---|
| Landing Page | http://localhost:8080/webapp/ |

Standardzugangsdaten: `demo` / `demo`

### Container stoppen und entfernen

```bash
docker stop cibseven
docker rm cibseven
```
