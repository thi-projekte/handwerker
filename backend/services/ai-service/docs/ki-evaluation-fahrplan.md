# KI-Evaluation — Fahrplan & Strategie

**Issue:** [#522 KI-Evaluation & Modellwahl](https://github.com/thi-projekte/handwerker/issues/522)
**Stand:** 2026-05-30
**Zweck dieses Dokuments:** Das übergeordnete Vorgehen festhalten — *was* wir evaluieren,
in *welcher Reihenfolge* und *warum*. Die Detail-Methodik (Kriterien, Gewichte, Szenarien)
steht im begleitenden [ki-evaluation-konzept.md](./ki-evaluation-konzept.md).

---

## 1. Die Kernfrage

> **Welches MegaLLM-Modell verwendet der ai-service — und arbeitet es zuverlässig mit
> der Produktdatenbank zusammen?**

Diese Frage zerfällt in **zwei eigenständige Geschichten**, weil der ai-service zwei
grundverschiedene LLM-Aufgaben hat. Beide sind wichtig, beide bekommen eine eigene,
ernsthafte Evaluation — nacheinander, nicht vermischt.

---

## 2. Warum zwei getrennte Evaluationen?

Der ai-service nutzt zwei LLM-Calls mit völlig unterschiedlichem Charakter:

| | **Call 1 — Extraktion** | **Call 2 — Produktauswahl** |
|---|---|---|
| Aufgabe | Sprachschnipsel → strukturierte Positionen | Pro Position das passende Produkt aus dem Katalog wählen |
| Schwierigkeit liegt bei … | dem **Modell** (Sprachverständnis) | der **Suche** (richtige Kandidaten finden) + dem Modell (auswählen) |
| Hängt ab von | nichts Externem | realistischem Produktkatalog + Vorfilter-Technik |
| Entscheidet primär über | **die Modellwahl** | **die Qualität der Produktanbindung** |

Würde man beide in einen Topf werfen, wüsste man am Ende nicht, *woran* ein schlechtes
Ergebnis lag — das macht die Entscheidung angreifbar. Getrennt ergeben sich **zwei klare,
verteidigbare Aussagen**.

---

## 3. Die zwei Geschichten

### 📖 Geschichte 1 — Modellwahl (JETZT, für die Team-Präsentation)

**Frage:** Welches der 7 Kandidaten-Modelle übersetzt Sprache am besten in saubere,
vollständige, korrekt strukturierte Angebotspositionen?

- **Fokus:** ausschließlich **Call 1**.
- **Unabhängig von** catalog-service, Auth, Embedding — kein externer Blocker.
- **Liefert:** die begründete Modellentscheidung (#537) + als Nebenprodukt die realen
  €/Angebot-Kosten für Cluster 1 (#132).
- **Ergebnis-Format:** Score-Board + Blind-Review im Team.

→ Das ist der Inhalt von [ki-evaluation-konzept.md](./ki-evaluation-konzept.md).

### 📖 Geschichte 2 — Produktanbindung (DANACH, eigenes Arbeitspaket)

**Frage:** Findet das System aus einem realistischen Katalog (mehrere hundert Artikel,
viele Near-Duplicates) zuverlässig das richtige Produkt?

- **Fokus:** **Vorfilter (Retrieval) + Call 2**.
- **Gehört zu** den Pipeline-Tickets #523 / #541 / #543 (Embedding-Vorfilter).
- **Wird ernst genommen, nicht vereinfacht:**
  - Realistischer Test-Katalog (~300–500 Artikel, synthetisch erzeugt, mit gezielten
    Near-Duplicates über mehrere Gewerke).
  - **Zwei Vorfilter-Wege im Vergleich:** Stichwort-Suche (lexikalisch) vs. Embedding-Suche
    (semantisch) → belegt mit Recall-Zahlen, welcher nötig ist. (Stuft #543 von „optional"
    zu einer datenbasierten Entscheidung hoch.)
  - Erst dann Call 2: aus den gefilterten Kandidaten das richtige Produkt wählen.
- **Misst zwei Dinge getrennt:**
  1. **Retrieval-Recall** — war der richtige Artikel überhaupt in den Kandidaten? (Vorfilter-Qualität)
  2. **Auswahl-Treffer** — hat das Modell aus den Kandidaten den richtigen gewählt? (Modell-Qualität)

> **Wichtige Erkenntnis:** „Das Modell connectet gut mit der Produktdatenbank" ist
> KEINE reine Modellfrage. Der schwierigere Teil ist die **Suche** (Vorfilter), die gar
> kein LLM ist. Deshalb verdient die Produktanbindung eine eigene Eval und wird nicht
> als Beilage der Modellwahl behandelt.

---

## 4. Warum diese Reihenfolge (erst Modell, dann Produkt)?

1. **Termin:** Die Modellentscheidung wird nächste Woche dem Team präsentiert. Geschichte 1
   ist sofort machbar, ohne auf catalog-service/Auth/Embedding zu warten.
2. **Abhängigkeit:** Die Produkt-Eval (Geschichte 2) profitiert davon, das Sieger-Modell
   aus Geschichte 1 bereits zu kennen — man testet die Produktauswahl dann mit dem (für
   Call 1) gewählten Modell und prüft, ob es auch hier passt oder ob für die simple
   Auswahl ein günstigeres Modell reicht.
3. **Klarheit:** Zwei fokussierte Evaluationen sind verständlicher und stärker
   verteidigbar als eine verworrene Gesamt-Eval.

---

## 5. Was Geschichte 2 NICHT ist

- Kein Grund, Geschichte 1 zu verzögern.
- Keine „später vielleicht"-Schublade — sie ist fest eingeplant als nächster Schritt
  nach der Modellwahl, mit realistischem Katalog und ehrlichem Vorfilter-Vergleich.

---

## 6. Zusammengefasst

| | Geschichte 1: Modellwahl | Geschichte 2: Produktanbindung |
|---|---|---|
| **Wann** | jetzt (Präsentation nächste Woche) | direkt danach |
| **LLM-Call** | Call 1 (Extraktion) | Call 2 (Auswahl) |
| **Kern-Herausforderung** | Sprachverständnis des Modells | Retrieval (Suche) + Auswahl |
| **Braucht Katalog?** | nein | ja, realistisch (~300–500) |
| **Braucht Embedding?** | nein | ja — im Vergleich zu Stichwort-Suche |
| **Tickets** | #522 (#535/#536/#537) | #523 / #541 / #543 |
| **Liefert** | Modellentscheidung + Kostenzahlen | Qualität der Produktanbindung |

---

## 7. Nächste Schritte (Geschichte 1)

1. [ki-evaluation-konzept.md](./ki-evaluation-konzept.md) auf Call 1 fokussieren
   (Call 2 als kurzer Ausblick auf Geschichte 2).
2. **Testdatenset (#535):** 5 Szenarien mit Sprachschnipseln + Gold-Referenzen festlegen.
3. **Smoke-Test:** ein Modell, ein Call gegen MegaLLM — Base-URL/Auth live verifizieren.
4. **Eval-Harness (#536):** Java/Quarkus, ruft alle Modelle über alle Szenarien auf.
5. **Lauf + Auswertung + Präsentation (#537):** Score-Board + Blind-Review.
