# 🖥️ Backend Architektur Leitfaden

Dieses Dokument definiert die Struktur und Regeln für das Backend unserer Anwendung. Ziel ist eine klare Trennung von Verantwortlichkeiten, eine wartbare Codebasis und eine saubere Zusammenarbeit mit dem Frontend-Team.

---

# 🧠 Grundprinzipien

Das Backend folgt einer strukturierten Schichtenarchitektur.

Ziele:

* klare Verantwortlichkeiten
* einfache Wartbarkeit
* saubere API-Struktur
* gute Skalierbarkeit
* einfache Erweiterbarkeit

---

# 📂 Projektstruktur

```
/backend
  /src
    /controllers
    /services
    /models
    /routes
    /middlewares
    /config
```

---

# 📁 /controllers

## Zweck

Controller nehmen HTTP-Requests entgegen und liefern HTTP-Responses zurück.

Sie bilden die Schnittstelle zwischen Frontend und Backend.

---

## Aufgaben

* Request entgegennehmen
* Eingaben validieren
* Service aufrufen
* Response zurückgeben

---

## Beispiele

```
/controllers
  authController.ts
  offerController.ts
```

---

## Regeln

### ✅ Erlaubt

* Request auslesen
* Response senden
* Fehler behandeln
* Service-Funktionen aufrufen

### ❌ Verboten

* Business-Logik
* Datenbankzugriffe direkt im Controller
* komplexe Berechnungen

---

## Beispiel

```ts
export const login = async (req, res) => {
  const result = await authService.login(req.body);

  res.status(200).json(result);
};
```

---

# 📁 /services

## Zweck

Enthält die Business-Logik des Backends.

Hier passiert die eigentliche Verarbeitung der Daten.

---

## Aufgaben

* Geschäftslogik
* Validierungen
* Datenverarbeitung
* Koordination zwischen Datenbank und API

---

## Beispiele

```
/services
  authService.ts
  offerService.ts
```

---

## Regeln

### ✅ Erlaubt

* Geschäftsregeln
* Datenverarbeitung
* Aufruf von Models

### ❌ Verboten

* HTTP-Responses senden
* direkte UI-Logik

---

## Beispiel

```ts
export const createOffer = async (data) => {
  const totalPrice = calculatePrice(data);

  return await OfferModel.create({
    ...data,
    totalPrice,
  });
};
```

---

# 📁 /models

## Zweck

Definiert Datenstrukturen und Datenbankmodelle.

Dieser Ordner repräsentiert die Datenbankebene.

---

## Aufgaben

* Datenbankmodelle definieren
* Datenbankzugriffe kapseln

---

## Beispiele

```
/models
  UserModel.ts
  OfferModel.ts
```

---

## Regeln

### ✅ Erlaubt

* Datenbankoperationen
* Schema-Definitionen

### ❌ Verboten

* Business-Logik
* HTTP-Logik

---

# 📁 /routes

## Zweck

Definiert alle API-Endpunkte.

Routes verbinden HTTP-Endpunkte mit den passenden Controllern.

---

## Beispiele

```
/routes
  authRoutes.ts
  offerRoutes.ts
```

---

## Beispiel

```ts
router.post('/login', authController.login);
```

---

## Regeln

### ✅ Erlaubt

* Routing definieren
* Middleware registrieren

### ❌ Verboten

* Business-Logik
* Datenbankzugriffe

---

# 📁 /middlewares

## Zweck

Middlewares verarbeiten Requests bevor sie den Controller erreichen.

---

## Typische Aufgaben

* Authentifizierung
* Logging
* Fehlerbehandlung
* Rate Limiting
* Request Validation

---

## Beispiele

```
/middlewares
  authMiddleware.ts
  errorMiddleware.ts
```

---

## Beispiel

```ts
export const authMiddleware = (req, res, next) => {
  if (!req.headers.authorization) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  next();
};
```

---

# 📁 /config

## Zweck

Zentrale Konfiguration der Anwendung.

---

## Beispiele

```
/config
  database.ts
  env.ts
```

---

## Inhalte

* Datenbankverbindung
* Environment Variablen
* globale Backend-Konfiguration

---

# 🔄 Typischer Request Flow

Ein Request durchläuft mehrere Schichten:

```text
Frontend Request
→ Route
→ Middleware
→ Controller
→ Service
→ Model
→ Datenbank
```

Antwort zurück:

```text
Datenbank
→ Model
→ Service
→ Controller
→ Frontend Response
```

---

# 📌 Beispiel: Login Flow

```text
POST /login
→ authRoutes.ts
→ authController.ts
→ authService.ts
→ UserModel.ts
→ Datenbank
```

---

# 📌 Beispiel: Angebot erstellen

```text
POST /offers
→ offerRoutes.ts
→ offerController.ts
→ offerService.ts
→ OfferModel.ts
→ Datenbank
```

---

# 🚨 Wichtige Regeln

## ❌ Verboten

* Datenbankzugriffe im Controller
* Business-Logik in Routes
* direkte Responses aus Services
* Vermischung von Verantwortlichkeiten

---

## ✅ Pflicht

* klare Trennung der Schichten
* Services für Business-Logik nutzen
* Controller möglichst klein halten

---

# 👥 Teamregeln

* Jede API-Änderung dokumentieren
* Einheitliche Namenskonventionen nutzen
* Keine Breaking Changes ohne Abstimmung

---

# 🔗 Zusammenarbeit mit Frontend

Frontend kommuniziert ausschließlich über APIs.

Das Frontend darf niemals direkt auf Datenbanklogik zugreifen.

Kommunikation erfolgt über:

```text
Frontend
→ HTTP Request
→ Backend API
→ Response
```

---

# 🎯 Ziel

Diese Struktur stellt sicher, dass:

* das Backend wartbar bleibt
* neue Features sauber integriert werden können
* die Zusammenarbeit im Team effizient bleibt
* Frontend und Backend sauber getrennt bleiben

---

Bei Unsicherheiten oder Architekturfragen bitte frühzeitig im Team abstimmen.