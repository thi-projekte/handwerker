# Services & Datenbank – Übersicht

## 🏗️ Architektur

Alle Microservices im CraftVoice-Projekt teilen sich **eine zentrale PostgreSQL-Instanz** in Docker. Jeder Service hat dabei seine eigene dedizierte Datenbank:

```
PostgreSQL-Container (postgres:15-alpine)
│
├── offers-db          ← offer-service
├── craftsman-db       ← craftsman-service (kommt später)
├── customer-db        ← customer-service (kommt später)
└── weitere-db         ← weitere Services (kommen später)
```

**Wichtig:** Jeder Service hat sein eigenes Datenbank-Schema und kann nur auf seine Datenbank zugreifen. Das garantiert Isolation und verhindert versehentliche Abhängigkeiten zwischen Services.

---

## 🐳 Docker Setup

### Normale Umgebung starten
Startet PostgreSQL im Docker-Netzwerk, Services können verbunden (z.B. von Kubernetes aus):

```bash
cd backend
docker compose up -d
```

### Entwicklungsumgebung starten
Startet PostgreSQL mit lokalem Port-Mapping. So kannst du von deinem Host-Computer über `localhost:5432` direkt auf die Datenbank zugreifen z.B. mit pgAdmin oder dbasync:

```bash
cd backend
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

### Container stoppen
```bash
docker compose down
```

### Container neu starten (z.B. nach Konfigurationsänderungen)
```bash
docker compose down && docker compose up -d
```

### Logs anschauen
```bash
docker compose logs -f postgres
```

---

## 🚀 Neuen Service hinzufügen

### Schritt 1: Service-Verzeichnis erstellen

```bash
cd backend/services
# Mit Quarkus CLI oder code.quarkus.io:
quarkus create app de.craftvoice:new-service-service
```

Oder manuell ein neues Quarkus-Projekt mit diesen **Extensions** initialisieren:
- Hibernate ORM with Panache
- REST Jackson
- SmallRye Health
- JDBC Driver - PostgreSQL
- OpenID Connect

### Schritt 2: pom.xml anpassen

```xml
<groupId>de.craftvoice</groupId>
<artifactId>new-service-service</artifactId>
<version>1.0.0</version>
<name>new-service-service</name>
```

### Schritt 3: application.properties konfigurieren

```properties
# Datenbankverbindung
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USER:postgres}
quarkus.datasource.password=${DB_PASSWORD:postgres}
quarkus.datasource.jdbc.url=jdbc:postgresql://${DB_HOST:postgres}:5432/${DB_NAME:new-service-db}

# Schema automatisch aktualisieren (nur Entwicklung)
# ⚠️ NICHT für Produktivsysteme geeignet
quarkus.hibernate-orm.database.generation=update

# Keycloak (wird später aktiviert nach Setup-Abschluss)
quarkus.oidc.tenant-enabled=false
# quarkus.oidc.auth-server-url=${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}
# quarkus.oidc.client-id=${KEYCLOAK_CLIENT_ID}
# quarkus.oidc.application-type=service
```

**Wichtig:** Der `DB_NAME` muss eindeutig sein und wird als New-Service-Identifikator genutzt.

### Schritt 4: PostgreSQL Datenbank erstellen

Bearbeite `backend/docker-compose.yml` und erstelle einen neuen Service für die neue Datenbank, falls erforderlich – oder nutze folgende Alternative:

**Option A (einfach):** Verbinde dich mit existierender PostgreSQL und erstelle die DB:

```bash
# Dev-Umgebung starten (mit lokalem DB-Zugriff)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Mit psql verbinden
psql -h localhost -U postgres -d postgres

# In psql:
CREATE DATABASE "new-service-db";
```

**Option B (automatisiert):** Nutze ein init-Script in `docker-compose.yml` (siehe Sektion "Datenbank-Initialisierung").

⚠️ **Wichtig:** PostgreSQL-Init-Skripte in Docker werden nur beim **ersten Start mit leerem Volume** ausgeführt.

Wenn PostgreSQL bereits läuft oder das Volume schon existiert, muss die Datenbank einmalig manuell erstellt werden:

```bash
psql -h localhost -U postgres -c 'CREATE DATABASE "offer-db";'
```

Alternativ kann das Volume gelöscht und PostgreSQL neu initialisiert werden.

### Schritt 5: Docker neu starten und testen

```bash
docker compose down
docker compose up -d

# Service-Logs kontrollieren
docker compose logs -f new-service
```

Falls der Service sich nicht mit PostgreSQL verbinden kann → siehe **Troubleshooting** unten.

---

## 📦 Java Package-Konvention

Alle Java-Klassen folgen dieser Struktur für Klarheit und Wartbarkeit:

```
de.craftvoice.<servicename>.<domain>.<classname>
```

### Beispiele

**offer-service:**
```
de.craftvoice.offerservice.offer.Offer                    (Entity)
de.craftvoice.offerservice.offer.OfferRepository          (Data Access)
de.craftvoice.offerservice.offer.OfferResource            (REST Endpoint)
de.craftvoice.offerservice.common.BaseEntity              (Shared)
```

**craftsman-service (zukünftig):**
```
de.craftvoice.craftsmanservice.craftsman.Craftsman
de.craftvoice.craftsmanservice.craftsman.CraftsmanRepository
de.craftvoice.craftsmanservice.craftsman.CraftsmanResource
```

**Regeln:**
- `<servicename>` ist der Verzeichnisname (z.B. `offerservice`, `craftsmanservice`)
- `<domain>` entspricht dem Business-Bereich (z.B. `offer`, `craftsman`, `payment`)
- Plurale werden NICHT verwendet
- Alle Package-Namen sind lowercase

---

## ⚠️ Wichtiger Hinweis zu Entitäten

### BPMN-Team-Abstimmung erforderlich
Bevor du Entitäten, Repositories oder REST-Ressourcen erstellst, **musst du mit dem BPMN-Team abstimmen**. Das Datenmodell muss mit dem Prozessmodell konsistent sein.

### JSON-String-Konvention
Alle Properties in Entitäten müssen **immer vorhanden sein**, auch wenn sie leer sind:

```java
@Entity
public class Offer {
    @Id
    public Long id;
    
    public String title;           // ✅ Immer ein Wert (auch "" oder null)
    public String description;     // ✅ Immer ein Wert
    public BigDecimal price;       // ✅ Immer ein Wert
    public String status;          // ✅ Immer ein Wert
}
```

**Warum?** Die JSON-Serialisierung muss für die Kompatibilität mit dem Prozessmodell garantieren, dass alle Felder im Output vorhanden sind:

```json
{
  "id": 123,
  "title": "Dachdeckung",
  "description": "Neue Eindeckung mit Dachziegeln",
  "price": 2500.00,
  "status": "PENDING"
}
```

Nie:
```json
{
  "id": 123,
  "title": "Dachdeckung"
  // ❌ Fehlende Properties
}
```

---

## 🔍 Debugging & Troubleshooting

### Problem: Service kann sich nicht zur Datenbank verbinden

**Prüfungen:**

1. **Läuft PostgreSQL?**
   ```bash
   docker compose ps
   ```
   Container `postgres` sollte mit Status `Up` angezeigt werden.

2. **Zeigt der Service Fehler?**
   ```bash
   docker compose logs postgres
   docker compose logs <service-name>
   ```

3. **Ist der Host-Name korrekt?**
   - In `application.properties`: `${DB_HOST:postgres}`
   - Der Container heißt: `postgres` ✅
   - Services im Docker-Netzwerk können sich über diesen Namen erreichen

4. **Ist die Datenbank erstellt?**
   ```bash
   psql -h localhost -U postgres -d postgres
   \l  # Listet alle Datenbanken
   ```

### Problem: Port 5432 ist bereits belegt

```bash
# Prüfe welcher Prozess Port 5432 nutzt
lsof -i :5432  # macOS/Linux
netstat -ano | findstr :5432  # Windows

# Änderung in docker-compose.dev.yml:
services:
  postgres:
    ports:
      - "5433:5432"  # Host-Port 5433, Container-Port 5432
```

Dann beim Verbinden `localhost:5433` nutzen.

### Problem: Hibernate erstellt Tabellen nicht automatisch

**Checklist:**

1. Ist `quarkus.hibernate-orm.database.generation=update` in `application.properties` gesetzt?
2. Sind Entitäten korrekt annotiert mit `@Entity` und `@Table`?
3. Wurden Entitäten in `resources/application.properties` registriert?
   ```properties
   quarkus.hibernate-orm.packages=de.craftvoice.offerservice
   ```

---

## 📚 Weiterführende Resources

- **[Quarkus Datenbank-Guide](https://quarkus.io/guides/datasource)**
- **[Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)**
- **[Docker Compose Referenz](https://docs.docker.com/compose/)**
- **[PostgreSQL Dokumentation](https://www.postgresql.org/docs/)**

---

## 💡 Best Practices

✅ **DO:**
- Ein Service = eine Datenbank
- Package-Namen konsistent halten
- Immer mit dem BPMN-Team abstimmen vor Entitäts-Erstellung
- Environment-Variablen nutzen (DB_HOST, DB_USER, DB_PASSWORD, DB_NAME)
- Dev- und Prod-Setup trennen (docker-compose.yml + docker-compose.dev.yml)

❌ **DON'T:**
- Direkter Datenbankzugriff zwischen Services (nur über REST-APIs)
- Tabellen in fremden Service-Datenbanken erstellen
- Fest codierte Verbindungsdaten (immer Umgebungsvariablen nutzen)
- Properties auslassen im Entity-Output (BPMN-Richtlinie)
- `quarkus.hibernate-orm.database.generation=create` in Produktion (Datenverlust!)

---

**Zuletzt aktualisiert:** 2026-05-11  
**Gültig für CraftVoice v1.0+**
