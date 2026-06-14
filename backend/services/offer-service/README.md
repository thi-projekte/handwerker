# 🛠️ Offer Service (Quarkus)

Willkommen im `offer-service`! Diese README erklärt, wie Quarkus, Docker, PostgreSQL und Hibernate zusammenarbeiten und wie du den Service lokal startest.

---

## 🏗️ 1. Die Bausteine (Wer macht was?)

Unser Setup besteht aus drei Hauptkomponenten:

1. **PostgreSQL (Die Datenbank):** Hier werden die Daten physisch gespeichert. Wir nutzen **Docker**, um eine vorkonfigurierte Datenbank ohne lokale Installation zu starten.
2. **Quarkus (Das Framework):** Unser Java-Backend. Es ist auf Schnelligkeit und geringen Ressourcenverbrauch optimiert.
3. **Hibernate ORM mit Panache (Der Übersetzer):** Verbindet die Java-Objektwelt mit der relationalen SQL-Datenbank. Panache minimiert dabei den benötigten Code (Boilerplate).

---

## ⚙️ 2. Wie funktioniert das Zusammenspiel?

### Schritt A: Die Datenbank läuft (Docker)
Die zentrale Datenbank-Konfiguration liegt im Verzeichnis `backend/docker-compose.dev.yml`. 
Wenn du die Datenbank startest, wird ein PostgreSQL-Container erstellt, der intern im Docker-Netzwerk als `postgres` erreichbar ist und lokal auf Port `5432` exponiert wird.

Die Datenbank für diesen Service heißt standardmäßig `offer-db`.

### Schritt B: Quarkus verbindet sich
In `src/main/resources/application.properties` ist die Verbindung definiert:
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://${DB_HOST:postgres}:5432/${DB_NAME:offer-db}
```
Lokal (außerhalb von Docker) kannst du `DB_HOST=localhost` setzen, um auf den Container zuzugreifen.

### Schritt C: Java-Klassen werden zu Tabellen (Hibernate)
Dank `quarkus.hibernate-orm.database.generation=update` erstellt Hibernate automatisch die passenden Tabellen in der Datenbank basierend auf deinen `@Entity`-Klassen (z.B. `Offer`).

---

## 🚀 3. Wie starte ich das Projekt zum Entwickeln?

Befolge diese Schritte, um die Entwicklungsumgebung zu starten:

1. **Docker-Daemon starten** (z.B. Docker Desktop).

2. **Deepgram API-Key konfigurieren:**
   Kopiere die Vorlage `.env.deepgram_example` in diesem Verzeichnis als `.env`:
   ```bash
   cp .env.deepgram_example .env
   ```
   Öffne die neue `.env`-Datei und trage deinen echten API-Key ein.
   Entweder nutzt du hierfür den bestehenden API-Key von Cluster 3 oder erstellst kostenlos einen eigenen unter https://console.deepgram.com/signup

3. **Datenbank starten** (aus dem Verzeichnis `backend/`):
   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```

4. **Service im Dev-Modus starten** (aus diesem Verzeichnis `backend/services/offer-service/`):
   ```bash
   ./mvnw quarkus:dev
   ```

5. **Verfügbarkeit prüfen**:
   Der Service läuft auf Port **8081**. Du kannst den Status hier abrufen:
   `http://localhost:8081/q/health`

**Vorteil des Dev-Modus:** Änderungen am Code werden sofort übernommen (Hot Reload), ohne dass du den Prozess neu starten musst.

---

## 🔧 4. Wichtige Konfigurationen (INFRA-1)

Der Service nutzt folgende wichtige Properties (konfigurierbar über Umgebungsvariablen oder die lokale `.env`-Datei):

*   **HTTP Port**: `8081` (Vermeidet Konflikt mit CIB seven auf 8080)
*   **Process Engine URL**: `${PE_URL:http://localhost:8080/engine-rest}`
*   **Deepgram API-Key**: `${DEEPGRAM_API_KEY:changeme}` (Lokal konfigurierbar über die `.env`-Datei)
*   **Datenbank**: Name `offer-db`, User `postgres`, PW `postgres` (Defaults für lokal)

---

## 🔒 5. Sicherheit & Profile

In der lokalen Entwicklung (Profil `dev`) sind viele Mechanismen wie Keycloak (`quarkus.oidc.tenant-enabled=false`) vorerst deaktiviert, um den Einstieg zu erleichtern. Für den Produktivbetrieb werden diese über entsprechende Profile oder Umgebungsvariablen aktiviert.

