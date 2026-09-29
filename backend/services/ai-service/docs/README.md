# AI-Service-Fachdokumente

Die Markdown-Dateien dieses Ordners enthalten Architekturentscheidungen, historische Modell-/Katalogevaluationen und den beschriebenen Message-Vertrag. Sie wurden zu unterschiedlichen Zeitpunkten im Mai/Juni 2026 erstellt und bilden nicht alle denselben Umsetzungsstand ab.

## Dokumente

| Datei | Thema | Einordnung |
|---|---|---|
| [`ki-uebersicht.md`](ki-uebersicht.md) | Aufgabenverteilung KI, Code, Datenbank; Preisgrenze | Fachliches Zielbild; technische Garantie durch Payload-Code prüfen |
| [`ergebnisKI-datenvertrag.md`](ergebnisKI-datenvertrag.md) | AI → Process Engine → Offer Service → Frontend | dokumentierter DTO-/BPMN-Vertrag, Stand 14.06.2026; enthält offene Punkte und historische Teambezüge |
| [`ai-service-modellwahl.md`](ai-service-modellwahl.md) | Modellvergleich und Modellwahl | Evaluation Stand 01.06.2026; Konfiguration `application.properties` zeigt aktuellen Runtime-Default |
| [`ki-evaluation-konzept.md`](ki-evaluation-konzept.md) | Call-1-Evaluationsmethodik | historisches Konzept; einige Aussagen beschreiben noch nicht implementierten Stand |
| [`ki-evaluation-fahrplan.md`](ki-evaluation-fahrplan.md) | zeitlicher Evaluationsplan | historischer Plan, keine aktuelle Roadmap |
| [`ki-evaluation-call2-konzept.md`](ki-evaluation-call2-konzept.md) | Katalogretrieval/Call-2-Auswertung | Ergebnisse Stand 01.06.2026; nicht als Produktionsabnahme verwenden |

## Quellenreihenfolge für technische Änderungen

1. aktueller Java-Code, `application.properties`, DTOs und Tests;
2. deploybare BPMN-Prozessmodelle der verantwortlichen Prozessgruppe;
3. die `ergebnisKI`-Vertragsbeschreibung, sofern mit aktuellem Code abgeglichen;
4. Eval-Berichte als begrenzte historische Evidenz.

Bei Änderungen an Prompts oder Retrieval immer prüfen, dass der KI-Payload keine Preis-, Stundensatz- oder Anfahrtsdaten enthält. Die Evaluationen beruhen auf projektinternen, kleinen und teilweise synthetischen Datensätzen; sie belegen keine Eignung für reale Angebote.

Für Einstieg und Betrieb siehe [AI Service README](../README.md) sowie das [Projekt-Handbuch](../../../../docs/PROJECT-HANDBOOK.md).

