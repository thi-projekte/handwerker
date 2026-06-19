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
  - **RBAC:** Rollenbasierte Zugriffskontrolle (OWNER, EMPLOYEE, ACCOUNTANT, CUSTOMER).
- **Deployment:** Docker & Docker Compose (Port 8082)

---

## 📑 Inhaltsverzeichnis
1. [Funktionsumfang](#-funktionsumfang)
2. [API-Endpunkte](#-api-endpunkte)
3. [Keycloak Integration](#-keycloak-integration)
4. [Service-zu-Service Kommunikation](#-service-zu-service-kommunikation)
5. [Datenmodell](#-datenmodell)
6. [Audit-Logging & DSGVO](#-audit-logging--dsgvo)

---

## 🚀 Funktionsumfang

### 1. Authentifizierung & Registrierung
- **Delegation:** Der Login erfolgt direkt gegen Keycloak. Der Service bietet lediglich einen Proxy-Endpunkt für die Registrierung an, um lokale Metadaten synchron zu halten.
- **Double-Opt-In:** Keycloak übernimmt den E-Mail-Verifizierungs-Workflow.

### 2. Benutzer- & Profilverwaltung
- **Synchronisation:** Beim Aufruf von `/me` werden Keycloak-Daten (Name, E-Mail) automatisch mit der lokalen Datenbank synchronisiert.
- **Metadaten:** Speicherung von Telefonnummern, Profilbildern und Unternehmensdaten (USt-IdNr, Handelsregister, Anschrift).

### 3. Kundenverwaltung
- **Kundenprofile:** Handwerker (`OWNER`, `EMPLOYEE`) können Kundenprofile anlegen, um diese in Angeboten zu verlinken.
- **Rollen:** Diese Nutzer erhalten die Rolle `CUSTOMER`.

### 4. KI-Personalisierung
- **Tone of Voice:** Einstellungen wie "Du" vs. "Sie" oder Detailgrade.
- **Textbausteine:** AGB-Hinweise und Zahlungsbedingungen für die KI-Angebotserstellung.

---

## 📡 API-Endpunkte & Frontend-Integration

Alle Endpunkte starten mit dem Präfix `/api/users`.

### 🔐 Authentifizierungs-Flow (WICHTIG für alle Teams)

#### Für das Frontend:
1. **Login:** Das Frontend nutzt `keycloak-js` (`keycloak.login()`) direkt gegen den Keycloak-Server.
2. **Autorisierung:** Nach dem Login muss der `Bearer <Token>` im `Authorization`-Header an alle gesicherten Endpunkte gesendet werden.
3. **Initialer Sync:** Das Frontend sollte nach dem Login einmalig `/api/users/me` aufrufen.

#### Für andere Backend-Services:
Wenn ein Service (z.B. `offer-service`) die Identität eines Nutzers prüfen muss:
1. **Token-Validierung:** Nutzt die Quarkus OIDC Extension (oder entsprechende Bibliotheken), um den JWT gegen Keycloak zu validieren.
2. **User-Details:** Falls Namen oder Firmendaten benötigt werden, kann der User-Service via ID abgefragt werden (Endpunkt in Planung) oder der Token-Inhalt genutzt werden.
3. **Rollenprüfung:** Rollen wie `OWNER` oder `CUSTOMER` sind im `realm_access.roles` Claim des JWT enthalten.

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
Es werden nur die Felder aktualisiert, die im Request gesendet werden (Teil-Update unterstützt).

- **Methode:** `PUT`
- **Pfad:** `/api/users/profile`
- **Body:**
```json
{
  "firstName": "Max",
  "lastName": "Mustermann",
  "phoneNumber": "+49 123 456789",
  "profilePictureUrl": "https://..."
}
```

#### 5. Firmendaten aktualisieren
Aktualisiert firmenspezifische Metadaten (nur für Nutzer mit Rolle `OWNER`).
Es werden nur die Felder aktualisiert, die im Request gesendet werden (Teil-Update unterstützt).

- **Methode:** `PUT`
- **Pfad:** `/api/users/company`
- **Body:**
```json
{
  "companyName": "Malerbetrieb Muster",
  "vatId": "DE123456789",
  "tradeRegisterNumber": "HRB 12345",
  "street": "Musterstraße",
  "houseNumber": "10",
  "zipCode": "12345",
  "city": "Musterstadt",
  "state": "Bayern",
  "country": "Deutschland",
  "companyEmail": "info@maler-muster.de",
  "companyPhoneNumber": "+49 89 12345",
  "website": "www.maler-muster.de",
  "industry": "Maler & Lackierer",
  "iban": "DE12 3456...",
  "bic": "GENO...",
  "bankName": "Musterbank",
  "accountHolder": "Max Mustermann",
  "taxNumber": "123/456/789",
  "legalForm": "Einzelunternehmen",
  "employeeCount": 5,
  "customerCount": 150,
  "hourlyRate": 65.50
}
```

#### 6. Profilbild hochladen
Lädt ein Profilbild hoch und speichert es serverseitig.

- **Methode:** `POST`
- **Pfad:** `/api/users/profile-picture`
- **Consumes:** `multipart/form-data`
- **Body:** `file` (Binary)
- **Response:** `200 OK` mit `{"url": "/api/users/profile-picture/profile_1_... .jpg"}`

#### 7. Kunden anlegen (Neu)
Erstellt ein Kundenprofil in der Datenbank. Nur für Handwerker erlaubt.

- **Methode:** `POST`
- **Pfad:** `/api/users/customers`
- **Roles:** `OWNER`, `EMPLOYEE`
- **Body:** (User-Objekt ohne `keycloakId`)
- **Response:** `201 Created`

#### 8. Kunden auflisten
Gibt eine Liste aller Profile mit der Rolle `CUSTOMER` zurück.

- **Methode:** `GET`
- **Pfad:** `/api/users/customers`
- **Roles:** `OWNER`, `EMPLOYEE`
- **Response:** `200 OK` (Array von User-Objekten)

#### 9. Einzelnen Kunden abrufen
Gibt die Details eines spezifischen Kunden zurück.

- **Methode:** `GET`
- **Pfad:** `/api/users/customers/{id}`
- **Roles:** `OWNER`, `EMPLOYEE`
- **Response:** `200 OK` (User-Objekt) oder `404 Not Found`

---

## 📊 Datenmodell (User-Objekt)

Dieses Objekt wird von `/me` zurückgegeben und bei `PUT` Requests genutzt.

### Vollständiges Modell
```json
{
  "id": 1,
  "email": "handwerker@beispiel.de",
  "firstName": "Max",
  "lastName": "Mustermann",
  "phoneNumber": "+49 170 1234567",
  "profilePictureUrl": "/api/users/profile-picture/profile_1.jpg",
  "status": "ACTIVE", 
  "roles": ["OWNER"],
  
  // Firmendaten
  "companyName": "Malerbetrieb",
  "vatId": "DE...",
  "tradeRegisterNumber": "HRB...",
  "street": "...",
  "houseNumber": "...",
  "zipCode": "...",
  "city": "...",
  "state": "...",
  "country": "...",
  "companyEmail": "...",
  "companyPhoneNumber": "...",
  "website": "...",
  "industry": "...",
  "iban": "...",
  "bic": "...",
  "bankName": "...",
  "accountHolder": "...",
  "taxNumber": "...",
  "legalForm": "...",
  "employeeCount": 5,
  "customerCount": 100,
  "hourlyRate": 60.0,
  "priceListUrl": null,

  // KI-Präferenzen
  "toneOfVoice": "Du",
  "detailLevel": "detailliert"
}
```

---

## 🛠 Service-zu-Service Kommunikation

Services sollten den **Authorization Header** bei internen Requests weiterreichen ("Token Propagation").

**Beispiel Quarkus RestClient:**
```java
@RegisterRestClient(configKey = "user-service")
@AccessToken // Trägt das aktuelle User-Token automatisch ein
public interface UserServiceClient {
    @GET
    @Path("/api/users/me")
    UserEntity getMe();
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
