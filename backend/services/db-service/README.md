# 🛠️ Database Service (Quarkus)

Willkommen im `db-service`! Wenn du dich fragst: *"Wie funktioniert hier eigentlich die Datenbankanbindung?"*, dann bist du hier genau richtig. Diese README erklärt einfach und verständlich, wie Quarkus, Docker, PostgreSQL und Hibernate zusammenarbeiten.

---

## 🏗️ 1. Die Bausteine (Wer macht was?)

Unser Setup besteht aus drei Hauptkomponenten:

1. **PostgreSQL (Die Datenbank):** Hier werden die Daten physisch gespeichert (Tabellen, Zeilen, Spalten). Da wir uns nicht die Mühe machen wollen, die Datenbank kompliziert auf unseren Laptops zu installieren, nutzen wir **Docker**.
2. **Quarkus (Das Framework):** Das ist unser Java-Backend. Quarkus ist superschnell, nimmt wenig Speicherplatz ein und bietet uns Werkzeuge, um REST-APIs bereitzustellen.
3. **Hibernate ORM mit Panache (Der Übersetzer):** Datenbanken sprechen `SQL`. Java spricht `Objekte`. Hibernate ist der "Übersetzer" zwischen diesen beiden Welten. Panache ist eine Quarkus-Erweiterung, die Hibernate noch einfacher macht, indem sie uns extrem viel unnötigen Code (Boilerplate) erspart.

---

## ⚙️ 2. Wie funktioniert das Zusammenspiel?

### Schritt A: Die Datenbank läuft (Docker)
In diesem Ordner liegt eine `docker-compose.yml`. Sie enthält den "Bauplan" für unsere PostgreSQL-Datenbank.
Wenn du `docker-compose up -d` ausführst, lädt Docker ein fertiges PostgreSQL-System herunter, startet es und öffnet Port `5432`. Die Datenbank heißt `demodb`.

### Schritt B: Quarkus verbindet sich
In der Datei `src/main/resources/application.properties` sagen wir Quarkus, wo die Datenbank liegt:
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/demodb
```
Sobald du Quarkus startest, liest es diese Konfiguration und baut eine permanente Verbindung zur laufenden Docker-Datenbank auf.

### Schritt C: Java-Klassen werden zu Tabellen (Hibernate)
Normalerweise müsstest du jetzt hergehen und SQL-Befehle schreiben (`CREATE TABLE Person...`). **Das müssen wir nicht tun!**
Dank der Einstellung `quarkus.hibernate-orm.database.generation=update` in den Properties passiert Folgendes:
Sobald Quarkus startet, schaut es sich deinen Java-Code an. Es sucht nach Klassen, über denen `@Entity` steht (z.B. die Klasse `Person.java`). 
Hibernate erkennt: *"Aha, es gibt eine Klasse `Person` mit den Feldern `name` und `role`. Ich schaue mal in der PostgreSQL-DB nach. Oh, es gibt noch keine Tabelle `Person`? Dann lege ich sie jetzt für dich an!"*

Das bedeutet: **Dein Java-Code steuert, wie die Datenbank aussieht.** Wenn du in Java ein neues Feld `public int age;` hinzufügst und Quarkus neu startest, fügt Hibernate automatisch die Spalte `age` in die Datenbanktabelle ein. *(Hinweis: Diesen Automatismus nutzt man nur in der Entwicklung. Später im Echtbetrieb nutzt man Tools wie Flyway, um die Datenbank kontrolliert zu updaten).*

---

## 🪄 3. Was ist "Panache"?

Wir haben unsere Klassen im Paket `de.winfprojekt.dbservice` angelegt (nicht mehr im Standard `org.acme`). 
Wenn du dir die `Craftsman.java` anschaust, siehst du, dass sie von `PanacheEntity` erbt:
```java
@Entity
public class Craftsman extends PanacheEntity {
    public String name;
    public String trade;
}
```

Weil sie von `PanacheEntity` erbt, erhält sie magische Superkräfte. Du musst keine komplizierten SQL-Abfragen schreiben.

Um über das Netzwerk (z.B. vom React-Frontend) echte Einträge anzulegen, baut man einen **REST-Endpunkt**. Schau dir dazu die Klasse `CraftsmanResource.java` an:

```java
@POST
@Transactional // Wichtig: Schreiboperationen in die DB brauchen eine Transaktion!
public Response create(Craftsman craftsman) {
    craftsman.persist(); // BÄM! Speichert den Handwerker in der DB.
    return Response.status(201).entity(craftsman).build();
}
```

Wenn du also echte Daten anlegen willst:
1. Erstelle eine Java-Klasse (Entity) im Ordner `src/main/java/de/winfprojekt/dbservice`.
2. Erstelle eine Resource-Klasse (wie `CraftsmanResource`), die auf HTTP-Anfragen reagiert.
3. Vergiss bei Schreibvorgängen (`persist`, `delete`, `update`) nicht die Annotation `@Transactional`!



---

## 🚀 4. Wie starte ich das Projekt zum Entwickeln?

Das ist der Workflow, den du jedes Mal brauchst, wenn du am Service arbeitest:

1. **Docker-Daemon starten** (z.B. Docker Desktop öffnen).
2. **Terminal in diesem Ordner öffnen** (`backend/services/db-service`).
3. **Datenbank starten:**
   ```bash
   docker-compose up -d
   ```
4. **Quarkus im Dev-Modus starten:**
   ```bash
   ./mvnw compile quarkus:dev
   ```

**Das Coole am Dev-Modus:** Quarkus läuft jetzt und überwacht deinen Code. Wenn du in Java etwas änderst und speicherst, lädt Quarkus den Code im Hintergrund sofort neu. Du musst den Server nicht jedes Mal stoppen und neu starten!

---

## ❓ Häufige Fragen

**Warum stehen in der Konfiguration Platzhalter wie `${DB_USER:postgres}`?**
Später im Produktivbetrieb (Deployment) laden wir das Programm auf einen Server. Dort gibt es kein `localhost` mehr und die Passwörter sind geheim. Die Syntax `${DB_USER:postgres}` bedeutet: *"Suche nach einer Umgebungsvariable `DB_USER`. Wenn du keine findest (weil wir lokal auf dem Laptop entwickeln), nimm als Standardwert einfach `postgres`."* So funktioniert der gleiche Code lokal und auf dem Server!
