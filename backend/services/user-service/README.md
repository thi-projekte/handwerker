# 👤 User Service - Dokumentation (Keycloak-basiert)

Der **User Service** ist das zentrale Modul für Identitätsmanagement, Authentifizierung, Autorisierung und Profilverwaltung innerhalb des Handwerker-Projekt-Ökosystems. Er nutzt **Keycloak** als primären Identity Provider und verwaltet zusätzlich anwendungsspezifische Metadaten.

---

## 🏗 Architektur & Technologien

- **Framework:** Quarkus (Java 17)
- **Identity Provider:** Keycloak (OIDC)
- **Datenbank:** PostgreSQL (via Hibernate Panache) - Speichert lokale Metadaten.
- **Sicherheit:** 
  - **OIDC (OpenID Connect):** Token-Validierung gegen Keycloak.
  - **Keycloak Admin Client:** Zur programmatischen Verwaltung von Usern.
  - **RBAC:** Rollenbasierte Zugriffskontrolle (OWNER, EMPLOYEE, ACCOUNTANT).
- **Deployment:** Docker & Docker Compose (Port 8082)

---

## 📑 Inhaltsverzeichnis
1. [Funktionsumfang](#-funktionsumfang)
2. [API-Endpunkte](#-api-endpunkte)
3. [Keycloak Integration](#-keycloak-integration)
4. [Datenmodell](#-datenmodell)
5. [Audit-Logging & DSGVO](#-audit-logging--dsgvo)

---

## 🚀 Funktionsumfang

### 1. Authentifizierung & Registrierung
- **Delegation:** Der Login erfolgt direkt gegen Keycloak. Der Service bietet lediglich einen Proxy-Endpunkt für die Registrierung an, um lokale Metadaten synchron zu halten.
- **Double-Opt-In:** Keycloak übernimmt den E-Mail-Verifizierungs-Workflow.

### 2. Benutzer- & Profilverwaltung
- **Synchronisation:** Beim Aufruf von `/me` werden Keycloak-Daten (Name, E-Mail) automatisch mit der lokalen Datenbank synchronisiert.
- **Metadaten:** Speicherung von Telefonnummern, Profilbildern und Unternehmensdaten (USt-IdNr, Handelsregister, Anschrift).

### 3. KI-Personalisierung
- **Tone of Voice:** Einstellungen wie "Du" vs. "Sie" oder Detailgrade.
- **Textbausteine:** AGB-Hinweise und Zahlungsbedingungen für die KI-Angebotserstellung.

---

## 📡 API-Endpunkte

Alle Endpunkte starten mit dem Präfix `/api/users`.

### Öffentliche Endpunkte

| Methode | Pfad | Beschreibung |
| :--- | :--- | :--- |
| `POST` | `/register` | Erstellt einen User in Keycloak und legt lokalen Rumpf-Datensatz an. |
| `POST` | `/password-reset/initiate` | Triggert die "Update Password" E-Mail von Keycloak. |

### Gesicherte Endpunkte (Bearer Token erforderlich)

| Methode | Pfad | Erforderliche Rolle | Beschreibung |
| :--- | :--- | :--- | :--- |
| `GET` | `/me` | Any | Synchronisiert Keycloak-Daten und gibt das Profil zurück. |
| `PUT` | `/profile` | Any | Aktualisiert persönliche Daten (lokal & in Keycloak). |
| `PUT` | `/company` | **OWNER** | Aktualisiert Firmendaten (lokal). |
| `DELETE` | `/` | **OWNER** | Löscht User in Keycloak und anonymisiert lokale Daten. |

---

## 🔐 Keycloak Integration

Der Service benötigt folgende Konfigurationen in Keycloak:
- **Realm:** `handwerker-realm`
- **Client:** `user-service` (Bearer-only oder Confidential)
- **Roles:** Die Rollen `OWNER`, `EMPLOYEE` etc. sollten als Client-Roles oder Realm-Roles gemappt sein.

---

## 🛡 Audit-Logging & DSGVO

### Transparenz
Sicherheitskritische Ereignisse werden in der `audit_logs` Tabelle protokolliert.

### DSGVO-Löschroutine
Bei Löschung eines Accounts:
1. Der Benutzer wird unwiderruflich aus **Keycloak** entfernt.
2. Der lokale Datensatz wird **anonymisiert** (Namen, E-Mail und Firmendaten werden genullt).
3. Der Status wird auf `DELETED` gesetzt.

---

*Erstellt von Gemini CLI - Stand: Mai 2026*
