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

## 📡 API-Endpunkte & Frontend-Integration

Alle Endpunkte starten mit dem Präfix `/api/users`.

### 🔐 Authentifizierungs-Flow (WICHTIG für Frontend)
Der User-Service übernimmt **nicht** den Login-Prozess. 
1. **Login:** Das Frontend nutzt `keycloak-js` (`keycloak.login()`) direkt gegen den Keycloak-Server.
2. **Autorisierung:** Nach dem Login muss der `Bearer <Token>` im `Authorization`-Header an alle gesicherten Endpunkte des User-Service gesendet werden.
3. **Initialer Sync:** Das Frontend sollte nach dem Login einmalig `/api/users/me` aufrufen, um das lokale Profil zu initialisieren/synchronisieren.

---

### Endpunkt-Details

#### 1. Benutzer Registrierung
Erstellt einen Account in Keycloak und einen Rumpf-Datensatz in der App-Datenbank. Triggert eine Verifizierungs-Mail.

- **Methode:** `POST`
- **Pfad:** `/api/users/register`
- **Body:**
```json
{
  "email": "handwerker@example.com",
  "password": "sicheresPasswort123",
  "firstName": "Max",
  "lastName": "Mustermann"
}
```
- **Response:** `201 Created`

#### 2. Passwort vergessen / Reset
Triggert den Keycloak-Standard-Workflow für Passwort-Resets.

- **Methode:** `POST`
- **Pfad:** `/api/users/password-reset/initiate`
- **Body:**
```json
{
  "email": "handwerker@example.com"
}
```
- **Response:** `200 OK` (Verschickt E-Mail via Keycloak)

#### 3. Eigenes Profil abrufen (Sync)
Gibt das aktuelle Profil zurück. Falls der User neu ist (z.B. nach externem Login), wird er hier automatisch in die lokale DB synchronisiert.

- **Methode:** `GET`
- **Pfad:** `/api/users/me`
- **Header:** `Authorization: Bearer <JWT>`
- **Response:** `200 OK` mit User-Objekt.

#### 4. Profil aktualisieren
Aktualisiert persönliche Stammdaten. Vornamen/Nachnamen werden automatisch zurück zu Keycloak synchronisiert.

- **Methode:** `PUT`
- **Pfad:** `/api/users/profile`
- **Body:** (siehe Datenmodell)

#### 5. Firmendaten aktualisieren
Aktualisiert firmenspezifische Metadaten (nur für Nutzer mit Rolle `OWNER`).

- **Methode:** `PUT`
- **Pfad:** `/api/users/company`
- **Body:** (siehe Datenmodell)

---

## 📊 Datenmodell (User-Objekt)

Dieses Objekt wird von `/me` zurückgegeben und sollte bei `PUT` Requests (teilweise) gesendet werden.

```json
{
  "id": 1,
  "email": "handwerker@example.com",
  "firstName": "Max",
  "lastName": "Mustermann",
  "phoneNumber": "+49 123 456789",
  "profilePictureUrl": "https://...",
  "status": "ACTIVE", // PENDING, ACTIVE, DELETED
  "roles": ["OWNER"], // OWNER, EMPLOYEE, ACCOUNTANT
  
  // Firmendaten (via /company)
  "companyName": "Musterbau GmbH",
  "vatId": "DE123456789",
  "tradeRegisterNumber": "HRB 12345",
  "companyAddress": "Musterstraße 1, 12345 Musterstadt",
  
  // KI-Präferenzen (Zukunft)
  "toneOfVoice": "DU", // DU, SIE
  "termsOfPayment": "Zahlbar innerhalb von 14 Tagen...",
  "disclaimer": "Angebot freibleibend..."
}
```

---

## 🛠 Entwicklung & Konfiguration

### Lokale Umgebung
- **Port:** `8082`
- **Basis-URL:** `http://localhost:8082/api/users`

### Passwort ändern
Es gibt keinen direkten API-Endpunkt für Passwortänderungen. Das Frontend sollte:
1. Den User zur **Keycloak Account Console** weiterleiten.
2. Oder den `/password-reset/initiate` Workflow nutzen.

### Rollen & Berechtigungen
Die Rollen `OWNER` und `EMPLOYEE` werden im JWT-Token von Keycloak erwartet. Der Service nutzt diese für `@RolesAllowed` Annotationen.

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
