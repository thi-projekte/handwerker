# 🖥️ Backend Architektur Leitfaden (Erweitert mit KI & Process Engine)

Dieses Dokument definiert die Struktur und Regeln für das Backend unserer Anwendung.
Die Architektur wurde erweitert, um den Einsatz von **Künstlicher Intelligenz (KI)** und einer **Process Engine (Workflow-System)** sauber zu integrieren.

Ziel ist eine klare Trennung von Verantwortlichkeiten, eine wartbare Codebasis und eine skalierbare Systemarchitektur.

Architekturstil: Erweiterte Schichtenarchitektur mit AI- und Workflow-Orchestrierung

---

# 🧠 Grundprinzipien

Das Backend folgt einer strukturierten Schichtenarchitektur mit klarer Verantwortungstrennung.

## Ziele

* klare Verantwortlichkeiten
* einfache Wartbarkeit
* saubere API-Struktur
* gute Skalierbarkeit
* einfache Erweiterbarkeit
* saubere Integration von KI und Workflows

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

    /ai            ← KI-Logik
    /workflows     ← Process Engine / Abläufe
    /integrations  ← externe APIs
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

## Regeln

### ✅ Erlaubt

* Request auslesen
* Response senden
* Fehler behandeln
* Service-Funktionen aufrufen

### ❌ Verboten

* Business-Logik
* KI-Aufrufe
* Workflow-Logik
* Datenbankzugriffe

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

Services enthalten die Business-Logik und fungieren als **Orchestrator**.

Sie koordinieren:

* Datenbank
* KI
* Workflows

---

## Aufgaben

* Geschäftslogik
* Koordination von Modulen
* Validierungen
* Datenverarbeitung

---

## Regeln

### ✅ Erlaubt

* Aufruf von Models
* Aufruf von AI-Modulen
* Starten von Workflows

### ❌ Verboten

* HTTP-Responses senden
* direkte UI-Logik

---

## Beispiel

```ts
export const createOffer = async (data) => {
  const offer = await OfferModel.create(data);

  const aiText = await ai.generateOfferText(data);

  await workflow.startOfferProcess(offer.id);

  return { ...offer, aiText };
};
```

---

# 📁 /models

## Zweck

Definiert Datenstrukturen und kapselt Datenbankzugriffe.

---

## Aufgaben

* Datenbankmodelle definieren
* CRUD-Operationen

---

## Regeln

### ✅ Erlaubt

* Datenbankoperationen
* Schema-Definitionen

### ❌ Verboten

* Business-Logik
* KI-Logik
* Workflow-Logik

---

# 📁 /routes

## Zweck

Definiert API-Endpunkte und verbindet sie mit Controllern.

---

## Regeln

### ✅ Erlaubt

* Routing
* Middleware einbinden

### ❌ Verboten

* Business-Logik
* Datenbankzugriffe

---

## Beispiel

```ts
router.post('/login', authController.login);
```

---

# 📁 /middlewares

## Zweck

Verarbeiten Requests vor dem Controller.

---

## Typische Aufgaben

* Authentifizierung
* Logging
* Fehlerbehandlung
* Rate Limiting
* Request Validation

---

# 📁 /config

## Zweck

Zentrale Konfiguration der Anwendung.

---

## Inhalte

* Datenbankverbindung
* Environment Variablen
* globale Einstellungen

---

# 🤖 /ai (KI-Schicht)

## Zweck

Kapselt alle KI-bezogenen Funktionen.

---

## Aufgaben

* Prompt-Erstellung
* Kommunikation mit KI APIs
* Response-Verarbeitung
* optionale Embeddings

---

## Regeln

### ✅ Erlaubt

* KI-Aufrufe
* Datenverarbeitung für KI

### ❌ Verboten

* direkte DB-Zugriffe
* HTTP-Logik

---

## Beispiel

```ts
export const generateOfferText = async (data) => {
  const prompt = buildPrompt(data);
  const response = await aiClient.call(prompt);
  return parseResponse(response);
};
```

---

# ⚙️ /workflows (Process Engine)

## Zweck

Steuert komplexe Geschäftsprozesse und Abläufe.

---

## Beispiele

* Bestellprozesse
* Genehmigungsflows
* Statusmaschinen
* Event-basierte Abläufe

---

## Regeln

### ✅ Erlaubt

* Starten und Steuern von Prozessen
* Interaktion mit Workflow-Engine

### ❌ Verboten

* direkte HTTP-Logik
* komplexe Business-Logik außerhalb von Prozessen

---

## Beispiel

```ts
export const startOfferProcess = async (offerId) => {
  return workflowEngine.start('offer-process', {
    offerId,
  });
};
```

---

# 🔌 /integrations

## Zweck

Kapselt externe Systeme und APIs.

---

## Beispiele

* KI-Anbieter
* Payment Provider
* E-Mail-Dienste
* Third-Party APIs

---

## Vorteil

* externe Abhängigkeiten sind isoliert
* einfacher Austausch möglich

---

# 🔄 Typischer Request Flow

```text
Frontend Request
→ Route
→ Middleware
→ Controller
→ Service
    → Model (DB)
    → AI Modul
    → Workflow Engine
→ Response
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

# 📌 Beispiel: Angebot mit KI + Workflow

```text
POST /offers
→ offerRoutes.ts
→ offerController.ts
→ offerService.ts
    → OfferModel.ts
    → ai/offerGenerator.ts
    → workflows/offerWorkflow.ts
→ Datenbank + Prozesse
```

---

# 🚨 Wichtige Regeln

## ❌ Verboten

* Datenbankzugriffe im Controller
* Business-Logik in Routes
* KI-Logik im Controller
* Workflow-Logik im Model
* direkte Responses aus Services
* Vermischung von Verantwortlichkeiten

---

## ✅ Pflicht

* klare Trennung der Schichten
* Services als Orchestrator nutzen
* KI sauber kapseln
* Workflows getrennt halten
* kleine, fokussierte Module

---

# 👥 Teamregeln

* Jede API-Änderung dokumentieren
* Einheitliche Namenskonventionen nutzen
* Keine Breaking Changes ohne Abstimmung
* KI- und Workflow-Änderungen besonders abstimmen

---

# 🔗 Zusammenarbeit mit Frontend

Frontend kommuniziert ausschließlich über APIs.

```text
Frontend
→ HTTP Request
→ Backend API
→ Response
```

❗ Das Frontend hat **keinen direkten Zugriff** auf:

* Datenbank
* KI-Logik
* Workflows

---

# 🎯 Ziel

Diese Architektur stellt sicher, dass:

* das Backend wartbar bleibt
* KI sauber integriert ist
* Prozesse klar steuerbar sind
* neue Features problemlos ergänzt werden können
* das Team effizient zusammenarbeitet

---

# 🧩 Fazit

Die Architektur basiert auf einer klassischen Schichtenstruktur, wurde jedoch gezielt erweitert:

* KI ist ausgelagert (`/ai`)
* Prozesse sind entkoppelt (`/workflows`)
* externe Systeme sind isoliert (`/integrations`)

➡️ Dadurch bleibt das System sauber, flexibel und skalierbar.

---

Bei Unsicherheiten oder Architekturfragen bitte frühzeitig im Team abstimmen.
