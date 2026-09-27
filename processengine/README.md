# Process Engine

`processengine/` baut eine CIB seven Enterprise (Camunda-basierte) Process Engine als Spring-Boot-Anwendung. BPMN-Ressourcen unter `src/main/resources/processes/` werden beim Start aus dem Classpath bereitgestellt. Die Engine orchestriert länger laufende Abläufe; Fachregeln und Angebotsdaten liegen in den Backend-Services.

## Struktur

```text
processengine/
├── src/main/java/.../loanapproval/   # Spring-Konfiguration, Security, HTTP-Connector-Ergänzungen
├── src/main/resources/processes/     # deploybare BPMN-Versionen
├── src/main/resources/application.yaml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

Der Java-Release ist 17. `pom.xml` bezieht CIB seven Enterprise `2.1.4-ee` aus einem authentifizierungspflichtigen Maven-Repository. Lokale Zugangsdaten werden über `~/.m2/settings.xml` konfiguriert und dürfen nicht versioniert werden. Der Process-Engine-CI-Workflow erwartet GitHub Actions Secrets `CIBSEVEN_USERNAME` und `CIBSEVEN_PASSWORD`.

## Lokaler Betrieb

1. Java 17, Maven und Docker bereitstellen.
2. Zugriff auf das CIB-seven-Enterprise-Maven-Repository einrichten.
3. Im Repository-Root:

   ```bash
   cd processengine
   mvn clean package -DskipTests
   docker compose up --build -d
   ```

`docker-compose.yml` bildet Port 8080 (anpassbar über `PROCESSENGINE_HTTP_PORT`) ab und erwartet ein bereits existierendes externes Docker-Netzwerk namens `nginx-proxy-manager`. Für isolierte lokale Ausführung muss das Compose-Netzwerk passend angepasst oder angelegt werden. Die Engine API befindet sich standardmäßig unter `/engine-rest`; die Weboberfläche wird durch diese Anwendungskonfiguration nicht automatisch freigeschaltet.

## Sicherheit und Grenzen

Die Anwendungskonfiguration deaktiviert Camunda-Autorisierung und verwendet einen festen OIDC-Issuer. Produktionsfähigkeit und konkrete Rollen-/Connector-Rechte müssen vor Betrieb geprüft werden. Das Repository allein beschreibt weder die vollständige Keycloak-Konfiguration noch betriebliche Backups, Datenbankmigrationen oder Wiederherstellung.

Die BPMN-Dateien in `docs/bpmn-reference/` sind Referenzkopien und laut dortiger README keine Single Source of Truth. Maßgebliche Prozessmodelle, Versionspolitik und die Prozessmodellierungsgruppe sind vor Änderungen zu bestätigen.

CI- und Betriebsdetails: [Übergabehandbuch](../docs/PROJECT-HANDBOOK.md).

