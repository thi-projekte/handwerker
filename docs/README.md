# Dokumentation

Diese Dokumentation beschreibt den technischen Prototyp im Repository-Stand vom 27.09.2026. Quellcode und Konfiguration sind bei Widersprüchen maßgeblich; offene oder extern zu bestätigende Punkte sind ausdrücklich markiert. KI-Systeme und neue Teammitglieder sollten zunächst das Root-README und dieses Handbuch lesen, bevor sie einzelne Service-READMEs als Implementierungsvertrag verwenden.

## Einstiegspunkte

- [Projektübersicht und Struktur](../README.md)
- [Betriebs- und Übergabehandbuch](PROJECT-HANDBOOK.md): Architektur, Hauptablauf, lokale Arbeit, Umgebungen, Deployment, Sicherheit, CI und offene Übergaben
- [Backend](../backend/README.md) und [Service-Index](../backend/services/README.md)
- [Frontend](../frontend/README.md)
- [Process Engine](../processengine/README.md)
- [BPMN-Referenzen](bpmn-reference/README.md)

## Service- und Detaildokumente

- [Offer Service](../backend/services/offer-service/README.md)
- [AI Service](../backend/services/ai-service/README.md), [Evaluation](../backend/services/ai-service/eval/README.md) und [KI-Fachdokumente](../backend/services/ai-service/docs/ki-uebersicht.md)
- [Catalog Service](../backend/services/catalog-service/README-catalog-service.md)
- [User Service](../backend/services/user-service/README.md)
- [Document Service](../backend/services/document-service/README-document-service.md)
- [Dashboard-API-Notiz](../backend/services/offer-service/src/main/java/de/winfprojekt/craftvoice/offerservice/dashboard/dashboard-backend-api.md) — Implementierung vor Änderungen gegenprüfen

## Begriffe

| Begriff | Bedeutung in diesem Repository |
|---|---|
| PE | Process Engine, die CIB-seven-Anwendung im Ordner `processengine/` |
| `businessKey` | Prozess-/Fachschlüssel zur Verknüpfung eines Angebots über asynchrone Schritte hinweg |
| `ergebnisKI` | JSON-String in der Message, mit der der AI Service das Ergebnis an die Process Engine korreliert |
| Katalogprodukt-ID | UUID des Materials; der AI Service gibt keine Preise aus |
| `main` | Aktueller Integrationsstand; einzelne Dateien enthalten historische Entscheidungen und können veraltet sein |

