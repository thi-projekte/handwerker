# 📱 Architektur Leitfaden
Dieses Dokument dient als verbindliche Orientierung für die Entwicklung unserer React-Anwendung. Ziel ist eine klare, skalierbare und wartbare Struktur, die von allen Teammitgliedern einheitlich genutzt wird.
---

# 🧠 Grundprinzipien
Unsere Architektur basiert auf drei zentralen Konzepten:

## 1. Feature-Based Struktur
Code wird nach Funktionen (Features) organisiert, nicht nach technischen Typen.
➡️ Weil: Klare Verantwortlichkeiten im Team

## 2. MVVM (Model-View-ViewModel)
Trennung von:

* UI (View)
* Logik (ViewModel)
* Daten (Model)
➡️ Weil: Sauberer, testbarer Code

## 3. Clean Architecture (leichtgewichtig)
Trennung von:

* Business-Logik
* UI
* Datenzugriff
➡️ Weil: Unabhängigkeit vom Backend

---

# 📂 Projektstruktur
```
/src
  /app
    /stylesheets
    /views
  /features
  /domain
  /data
  /shared
  /core
  /assets
```
---
# 📁 /app
## Zweck
Zentrale Steuerung der Anwendung

## Inhalt

* `App.tsx` → Einstiegspunkt
* `routes.tsx` → Navigation
* `providers.tsx` → Globale Zustände (Auth, Theme, etc.)
* `stylesheets/` → App-Styles und globale CSS-Dateien
* `views/` → HTML-Seiten und statische App-Views

## Regeln

* Keine Business-Logik
* Keine Feature-spezifischen Implementierungen

---
# 📁 /features
## Zweck
Enthält alle funktionalen Bereiche der App

## Struktur eines Features
```
featureName/
  components/
  hooks/
  services/
  models/
  index.ts
```
## Regeln
* Jedes Feature ist in sich abgeschlossen
* Keine direkte Kommunikation zwischen Features
* Kommunikation erfolgt über Domain oder Shared
---
## 📂 components
UI-Komponenten (React Components)

### Regeln
* Keine Business-Logik
* Nur Darstellung + Events
---
## 📂 hooks
ViewModel (Logik der Anwendung)

### Aufgaben
* State Management
* Aufruf von Use Cases
* Datenverarbeitung für UI

### Regeln
* Keine direkte API-Kommunikation
* Keine komplexe Business-Logik
---
## 📂 services
Technische Implementierungen

### Beispiele
* Zugriff auf Browser APIs (z. B. Mikrofon)
* Drittanbieter-Bibliotheken

### Regeln
* Keine Business-Logik
---
## 📂 models
Feature-spezifische Datentypen
---

## 📄 index.ts
Exportiert die öffentliche API des Features
---
# 📁 /domain -> Schnittstelle mit Backend
## Zweck
Zentrale Business-Logik der Anwendung
## Struktur
```
/models
/usecases
```
---
## 📂 models
Globale Datenmodelle

### Beispiele
* Offer
* Form
* VoiceInput
---
## 📂 usecases
Business-Logik

### Beispiele
* generateOffer.ts
* parseVoiceInput.ts
* validateForm.ts

### Regeln
* Kein React
* Keine API Calls
* Reine Funktionen
---
# 📁 /data
## Zweck
Kommunikation mit externen Systemen (Backend)

## Struktur
```
/api
/repositories
```
---

## 📂 api
### Inhalte
* `apiClient.ts` → zentrale API-Konfiguration
* `endpoints.ts` → API-Endpunkte
---

## 📂 repositories
### Zweck
Vermittler zwischen Domain und API

### Regeln
* Nur hier werden API Calls gemacht
---
# 📁 /shared
## Zweck
Wiederverwendbare Komponenten und Logik

## Struktur
```
/components
/hooks
/utils
/types
```
---
## Regeln
* Darf von allen Features genutzt werden
* Keine Feature-spezifische Logik
---
# 📁 /core
## Zweck
Technische Grundlagen der Anwendung

## Struktur
```
/config
/constants
```
---
## Inhalte
* Environment Variablen
* Globale Konstanten
---
# 📁 /assets
## Zweck
Statische Dateien

### Beispiele
* Bilder
* Icons
---
# 🚨 Wichtige Regeln
## ❌ Verboten
* Business-Logik in React Components
* API Calls außerhalb von /data
* Direkte Abhängigkeiten zwischen Features

## ✅ Pflicht
* Nutzung von Hooks für Logik
* Nutzung von Use Cases für Business-Regeln
* Klare Trennung der Verantwortlichkeiten
---
# 🔄 Typischer Datenfluss
1. User interagiert mit UI
2. Component ruft Hook auf
3. Hook nutzt Use Case
4. Use Case verarbeitet Daten
5. Repository kommuniziert mit API
6. Ergebnis zurück zur UI
---
# 👥 Teamregeln
* Jedes Feature hat einen Verantwortlichen
* Code Reviews sind Pflicht
* Gemeinsame Namenskonventionen einhalten
---
# 🎯 Ziel
Diese Struktur stellt sicher, dass:

* die App skalierbar bleibt
* der Code verständlich ist
* neue Features schnell entwickelt werden könnens
---

Bei Fragen oder Unsicherheiten bitte frühzeitig im Team klären, sodass die Struktur des Projektes konsistent bleibt