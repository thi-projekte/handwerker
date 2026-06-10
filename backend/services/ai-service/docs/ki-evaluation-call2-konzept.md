# KI-Evaluation Call 2 — Konzept (Produktauswahl & Vorfilter)

**Issue:** [#523 LLM-Pipeline Erstangebot](https://github.com/thi-projekte/handwerker/issues/523)
(relevant: #540 Catalog-REST-Client, #541 LLM-Call 2 Produktauswahl, #543 Embedding-Vorfilter)
**Stand:** 2026-06-01
**Vorgänger:** [ki-evaluation-konzept.md](./ki-evaluation-konzept.md) (Call 1, abgeschlossen) ·
[ki-evaluation-fahrplan.md](./ki-evaluation-fahrplan.md) (Geschichte 2)

Dieses Dokument löst den knappen Call-2-Teil im Hauptkonzept ab und macht aus der
„Geschichte 2 — Produktanbindung" des Fahrplans ein konkretes, durchführbares Vorgehen.

---

## 1. Worum es geht

Nach Call 1 liegen **strukturierte Positionen** vor (z. B. *„Laminat verlegen, 25 m²,
Eiche, 8 mm"*). Call 2 ordnet jeder Material-Position das **passende echte Produkt aus
dem Katalog** zu. Klingt simpel, ist es aber nicht — und zwar aus einem Grund, der die
ganze Methodik prägt:

> **Die Schwierigkeit liegt überwiegend bei der *Suche*, nicht beim Modell.**
> „Das System connectet gut mit der Produktdatenbank" ist zu **~80 % ein Retrieval-Problem**
> (welche Kandidaten findet die Suche?) und nur zu ~20 % eine Modellfrage (wählt das Modell
> aus den Kandidaten richtig?). Deshalb messen wir **beides getrennt**.

**Datenschutz-Constraint (unverhandelbar):** Der KI werden **keine Preise** übergeben.
Sie bekommt nur Produktbeschreibungen + Referenz (`articleNumber`) und gibt eine Referenz
zurück. Den Preis ergänzt nachgelagert der offer-/catalog-service.

---

## 2. Warum ein eigener synthetischer Katalog — und nicht Michis #594?

Wir haben Michis catalog-service (PR #594) geprüft. Ergebnis differenziert:

**Was gut ist und übernommen wird:** Das `Material`-Datenmodell ist realistisch und am
deutschen Standard **DATANORM** orientiert. Wir bauen unseren Eval-Katalog **exakt in
diesem Schema** — dann überträgt sich die Eval später 1:1 auf den echten Service.

```
Material: id, ownerId, articleNumber, name, description,
          supplierNumber, supplierName, categoryCode, categoryName,
          unit, priceNet, priceGross, vatRate, currency, source, active
```

**Warum wir #594 trotzdem nicht direkt für die Eval nehmen:**

| Grund | Detail |
|---|---|
| **Zu wenig Daten** | `import.sql` enthält aktuell nur **3 Artikel** — unbrauchbar für einen Retrieval-Test. |
| **Keine Suche vorhanden** | catalog-service kann derzeit nur `getAll` / `getById` / CRUD / CSV-/DATANORM-Import — **keinen Textsuche-Endpunkt**. Der Vorfilter existiert also nirgends; **genau ihn evaluieren wir.** |
| **Gold braucht Kontrolle** | Für belastbare Trefferquoten müssen *wir* die richtige Antwort pro Testfall kennen → nur mit einem Katalog möglich, dessen Inhalt wir steuern. |
| **Near-Duplicates gezielt** | Der Test wird erst aussagekräftig, wenn wir Stolperfallen (s. u.) bewusst einbauen — das geht mit echten Zufallsdaten nicht. |
| **Auth offen** | Zugriff auf den echten Service braucht JWT/M2M-Token — ungeklärt (siehe Auth-Memo). |

➡️ **Entscheidung:** synthetischer Katalog **im Material-Schema**, kontrolliert befüllt,
mit gezielten Near-Duplicates. Realistisch *und* auswertbar.

---

## 3. Der Katalog — Design

**Größe:** ~**400 Artikel** (Zielkorridor 300–500). Groß genug, dass naive Suche an
Grenzen kommt; klein genug, um synthetisch sauber erzeugbar und prüfbar zu bleiben.

**Felder & KI-Sichtbarkeit:**

| Feld | Für Retrieval (Suche) | An die KI (Call 2) | Gold/ID |
|---|:---:|:---:|:---:|
| `name`, `description` | ✅ Haupttext | ✅ | |
| `categoryCode`, `categoryName` | ✅ | ✅ | |
| `unit` | ✅ (Mengen-Plausibilität) | ✅ | |
| `articleNumber` | (ID) | ✅ (als Referenz) | ✅ Gold-Schlüssel |
| `supplierName`, `supplierNumber` | optional | optional | |
| **`priceNet`, `priceGross`, `vatRate`** | — | **❌ NIE** | |

**Gewerke-Abdeckung** (an die Call-1-Szenarien angelehnt, damit ein durchgehender Flow
demonstrierbar ist):

| Gewerk | ca. Anteil | Beispiel-Artikel |
|---|---|---|
| Bodenbeläge | 30 % | Laminat, Vinyl/Designboden, Parkett, Trittschalldämmung |
| Fliesen & Verlegung | 20 % | Boden-/Wandfliesen (Formate), Fliesenkleber, Fugenmörtel, Kreuze |
| Sanitär | 20 % | bodengleiche Duschelemente, Abläufe/Rinnen, Abdichtung, Armaturen |
| Trockenbau/Leisten | 15 % | Sockel-/Fußleisten, Dämmplatten, Profile |
| Maler | 10 % | Wandfarbe, Grundierung, Putz |
| Kleinmaterial | 5 % | Silikon, Dübel, Schrauben |

### Die Near-Duplicate-Strategie (das Herzstück)

Ein Retrieval-Test ist nur so gut wie seine Stolperfallen. Wir bauen **sechs** Sorten
gezielt ein — sie sind genau die Fälle, an denen naive Suche scheitert:

1. **Dimensions-Varianten** — Laminat 7/8/10/12 mm · Fliese 30×30 / 60×60 / 80×80 · Rohr DN50/70/100.
   *(Position nennt exakte Größe → nur die richtige Variante zählt.)*
2. **Dekor-/Farb-Varianten** — Eiche natur/grau/hell/rustikal; Weiß/Anthrazit.
3. **Marken-/Lieferanten-Varianten** — gleiche Funktion, anderer `supplierName`/`articleNumber`.
4. **Synonyme & Umgangssprache ↔ Katalogname** — „Klick-Laminat" vs. „Laminatboden Klicksystem";
   „Sockelleiste" vs. „Fußleiste"; „bodengleiche Dusche" vs. „Duschelement bodeneben, befliesbar".
   *(Tötet reine Stichwort-Suche — Hauptargument fürs Embedding.)*
5. **Keyword-Fallen / Distraktoren** — „Laminatboden" vs. „Laminat-Reiniger" vs. „Laminat-Reparaturset";
   „Fliesenkleber" vs. „Fliese". *(Gleiches Stichwort, falsches Produkt.)*
6. **Einheiten-/Gebinde-Varianten** — pro m² vs. pro Paket vs. pro Stück; 5-kg- vs. 25-kg-Sack.

Pro Gold-Artikel platzieren wir bewusst **3–8 Near-Duplicates** im selben Katalog.

---

## 4. Der Vorfilter — was & warum

**Definition:** Der Vorfilter ist der **Suchschritt**, der aus dem Gesamtkatalog (~400)
für eine Position die **Top-k Kandidaten** (Vorschlag: **k = 15**) auswählt, die der KI
für Call 2 vorgelegt werden.

**Warum überhaupt nötig?**
- **Token/Kosten:** 400 Artikel passen nicht sinnvoll in einen Prompt — zu teuer, zu langsam.
- **Qualität:** Modelle werden mit sehr vielen Optionen messbar schlechter („lost in the middle").
- **Architektur:** Der echte catalog-service hat (noch) keine Suche — wir müssen ohnehin
  entscheiden und bauen, *wie* gesucht wird.

**Der entscheidende Punkt — der Vorfilter setzt die Obergrenze:**

> Ist der richtige Artikel **nicht** unter den k Kandidaten, kann **kein** Modell ihn mehr
> wählen — egal wie gut. Die **Retrieval-Recall** ist damit die Decke für die gesamte
> Call-2-Qualität. Genau deshalb messen wir sie als **eigene** Größe (Abschnitt 6).

### Zwei Vorfilter-Wege — Reihenfolge & Begründung

Wir vergleichen **lexikalische Stichwort-Suche** vs. **semantische Embedding-Suche** —
aber **gestaffelt: erst Stichwort (Baseline), dann Embedding.** Begründung:

1. **Baseline-Pflicht:** Der Mehrwert von Embeddings ist nur als Differenz zu einer
   Baseline aussagekräftig. Ohne die Stichwort-Recall-Zahl kann man „+X % durch Embedding"
   gar nicht behaupten.
2. **Vielleicht reicht Stichwort:** Erreicht die lexikalische Suche schon ~95 % Recall,
   ist „Embedding ist unnötig" selbst ein wertvolles Ergebnis — und beantwortet exakt die
   offene Frage von **#543** (dort als „optional" markiert).
3. **Infra-Risiko bewusst danach:** Stichwort braucht **keine** neue Infrastruktur.
   Embedding braucht ein Embedding-Modell (über MegaLLM) + Ähnlichkeitssuche — das bauen
   wir gezielt als zweiten Schritt, nicht als Einstiegshürde.
4. **Harness von Anfang an umsteckbar:** Der Retriever ist eine austauschbare Komponente
   (`keyword` ↔ `embedding`), damit der Vergleich ohne Umbau läuft.

*Die Near-Duplicate-Fallen Typ 4 (Synonyme) und Typ 5 (Distraktoren) sind die Stellen, an
denen die Stichwort-Suche voraussichtlich einbricht — dort entscheidet sich, ob Embedding
seinen Aufwand wert ist.*

---

## 5. Das Datenset — Szenarien (Position → Gold-Artikel)

**Eingabe je Szenario:** eine einzelne Material-Position im Stil eines Call-1-Outputs
(`bezeichnung`, `menge`, `einheit`, ggf. `beschreibung`).
**Gold:** die korrekte `articleNumber` — oder ein **Set akzeptabler** Referenzen, wo
mehrere gleichwertig sind.

**Umfang:** **16 Szenarien** (mehr als die im Ticket genannten „mind. 5", für statistische
Aussagekraft), verteilt über die Gewerke und die sechs Fallen-Typen. Aktueller Stand:
A 1 · B 4 · C 3 · D 4 · E 2 · F 2 (siehe `docs/eval-datenset-call2.json`).

| Typ | Szenario-Charakter | Prüft |
|---|---|---|
| **A — Eindeutig** | genau ein klar passender Artikel | Grund-Matching |
| **B — Dimensions-Feinunterschied** | 8 mm vs. 10 mm; 60×60 vs. 30×30 | exakte Attribut-Treue |
| **C — Synonym/Umgangssprache** | Position umgangssprachlich, Katalog technisch | Verständnis jenseits der Wortgleichheit |
| **D — Distraktor-Falle** | mehrere teilen das Stichwort, nur einer passt | Unterscheidung Produkt vs. Zubehör |
| **E — Kein guter Treffer** | Katalog enthält das Gesuchte nicht | **ehrliches „kein Treffer"/Rückfrage statt falsch zu wählen** |
| **F — Mehrere akzeptabel** | mehrere gleichwertige Varianten | sinnvolle, nicht-falsche Wahl (Gold-Set) |

Typ **E** ist das Call-2-Pendant zum „Ask-vs-Guess" aus Call 1: Ein gutes Modell **erfindet
keine** Zuordnung, sondern meldet „kein passendes Produkt gefunden".

---

## 6. Metriken — zwei getrennte Messungen

### 6a. Vorfilter-Eval (modellunabhängig)

Misst **nur die Suche**, kein LLM beteiligt:

| Metrik | Definition |
|---|---|
| **Recall@k** | War der Gold-Artikel unter den Top-k Kandidaten? (Anteil — **nur Szenarien mit Gold**) |
| **Recall@k pro Fallen-Typ** | Wo bricht die Suche ein? (erwartet: Typ 4/5 bei Stichwort) |
| (später) **Δ Embedding** | Recall-Gewinn der semantischen gegenüber der lexikalischen Suche |

> **Scorer-Regel (wichtig):** Typ-**E**-Szenarien (kein Treffer) haben **kein Gold** →
> Recall@k ist dort **undefiniert** und wird aus dem Recall-Nenner **ausgeschlossen**.
> Sonst verschmutzt es die modellunabhängige Such-Messung. Bei Typ E messen wir
> stattdessen: (1) **Köder-Präsenz** — landet der naheliegende Distraktor (Aufputz-Armatur
> bzw. Marmoroptik-Keramik) in den Top-k? — und (2) ob das **Modell ihn trotzdem ablehnt**
> (das fließt in „Korrekt-kein-Treffer", 6b).

### 6b. Call-2-Modell-Eval (pro Modell)

Misst die **Auswahl** — und zwar fair: das Modell bekommt Kandidaten, die den Gold-Artikel
**enthalten** (sonst würde man Such- und Modellfehler vermischen):

| # | Kriterium | Gewicht | Wie gemessen |
|---|---|---|---|
| 1 | **Auswahl-Treffer (Accuracy)** | 55 % | richtige `articleNumber` gewählt? (vollautomatisch gegen Gold) |
| 2 | **Korrekt-kein-Treffer** | 15 % | bei Typ-E-Fällen ehrlich „keiner passt" statt falsch zu wählen? |
| 3 | **Format-Treue** | 15 % | genau eine gültige Referenz, **kein Preis**, keine Halluzination |
| 4 | **Latenz & Kosten** | 15 % | Antwortzeit + Token (≈ Cent) |

**K.-o. (Lauf = 0):** erfundene `articleNumber` (nicht im Katalog) · ausgegebener Preis.

Diese Trennung ist der Kern: **6a** sagt „wie gut ist unsere Suche", **6b** sagt „welches
Modell wählt am besten" — und ob (wie bei Call 1) ein günstiges Modell reicht.

---

## 7. Ablauf

1. **Katalog erzeugen** — ~400 Artikel synthetisch im Material-Schema, mit eingebauten
   Near-Duplicates → `eval/data/catalog-call2.json` (eingefroren, versioniert).
2. **Datenset festlegen** — 16 Szenarien + Gold(-Sets) → `docs/eval-datenset-call2.json`.
3. **Vorfilter-Lauf (Stichwort)** — Recall@k messen, pro Fallen-Typ aufschlüsseln.
4. **Call-2-Modell-Lauf** — pro Modell × Szenario × N Runs, Kandidaten enthalten Gold;
   Accuracy/Format/Latenz automatisch werten.
5. **Embedding-Vorfilter (Schritt 2)** — gleiche Recall-Messung, Vergleich zu Schritt 3
   → datenbasierte Antwort auf #543.
6. **Report** — Recall-Vergleich (Vorfilter) + Modell-Score-Board (Auswahl), analog zum
   Call-1-Report.

---

## 8. Warum diese Methodik nicht angreifbar ist

- **Hart & maschinell:** Recall und Auswahl-Treffer sind 100 % gegen Gold berechenbar,
  reproduzierbar, keine Meinung nötig.
- **Sauber getrennt:** Such-Qualität und Modell-Qualität werden nie vermischt — jede Zahl
  hat eine eindeutige Ursache.
- **Realistisch & kontrolliert zugleich:** echtes Material-Schema (DATANORM/#594) + bewusst
  gesetzte Stolperfallen → praxisnah *und* auswertbar.
- **Architektur-ehrlich:** evaluiert genau den noch zu bauenden Schritt (es gibt heute
  keine Suche) und bezieht den Datenschutz (keine Preise) als Pflicht ein.
- **Reproduzierbar:** eingefrorener Katalog + Datenset + fixe Parameter.

---

## 9. Offene Entscheidungen (vor der Umsetzung zu klären)

| Punkt | Vorschlag |
|---|---|
| **Welches Modell für Call 2?** | Sieger aus Call 1 als Start + ein günstiges gegentesten (Hypothese: simple Auswahl braucht kein Premium-Modell) — nach der Dienstag-Präsi/#537. |
| **k (Kandidatenzahl)** | 15 (Trade-off Recall ↔ Promptgröße); ggf. Recall@5/@10/@15 vergleichen. |
| **Embedding-Modell** | über MegaLLM verfügbares Embedding-Modell prüfen (Verfügbarkeit + Preis). |
| **Katalog-Größe final** | 400 als Startwert; bei Bedarf auf 500 erhöhen. |
| **Katalog: handkuratiert vs. generiert** | Hybrid — Gold-Artikel + ihre Near-Duplicates handkuratiert, Rest als Füll-Rauschen generiert. |

---

## 10. Ergebnisse & Befund (Stand 2026-06-01)

Durchgeführt: synthetischer Katalog (400 Artikel), 16 Szenarien, BM25-Vorfilter, eine
reine Programmier-Auswahl-Baseline und ein LLM-Lauf (4 Modelle × 16 × 5 Runs = 320 Calls).

### 10a. Vorfilter (Stichwort, BM25)
- **Recall@5 = 93 % · Recall@10 = 93 % · Recall@15 = 100 %** (14 Szenarien mit Gold).
- 13/14 Gold-Artikel schon auf Rang 1–2. Einziger Ausreißer: **S6** (Rang 13) —
  Terminologie-Lücke „Fugenkreuze" (Position) vs. „Fliesenkreuze" (Katalog).
- „Kein Treffer"-Köder (S9 Aufputz-Armatur, S15 Marmoroptik-Keramik) liegen auf Rang 1 →
  der verlockende Falschtreffer wird dem Modell garantiert vorgelegt.

### 10b. Reine Programmierung (ohne KI)
- **S0 Top-1:** 11/16. **S1 Top-1 + Attribut-Matching:** **15/16**.
- Einziger Fehler von S1: **S15** (Marmoroptik). Regeln matchen die Wörter
  *marmor/poliert/naturstein* und greifen den Köder — „Marmoroptik ≠ Echtmarmor" ist
  Bedeutung, keine Regel. Zudem: S9 klappte nur dank handgebauter Regel „Unterputz ≠ Aufputz".

### 10c. LLM-Auswahl (Call 2)
- **Alle 4 Modelle 100 %** (16/16), stabil über **5 Runs**: **311 erfolgreiche Calls, 0 falsche.**
  Inkl. **S15** (das Programmier-K.o.) und **beide Ablehnungen** (S9/S15) zuverlässig.
- **Teuer ≠ besser (erneut):** `gemma-4-26b` (0,15/0,60 USD) = `claude-opus-4-8` (5/25),
  aber ~16× billiger; opus hatte nur 9× HTTP-503 (Infrastruktur, kein Auswahlfehler).
- Kosten: ~0,05 Cent pro Position mit gemma.

### 10d. Schlussfolgerung
- **Call 2 lohnt sich — mit dem billigsten Modell.** Mehrwert ggü. Programmierung ist v.a.
  **Generalisierung ohne Regelpflege** + korrektes „passt nichts". (Bezug zu Emanuels
  Frage „2 Calls → 1": Call 2 ist so billig/robust, dass eine Zusammenlegung nicht nötig ist.)
- **Die eigentliche Schwierigkeit ist das Retrieval, nicht das Modell.** Künftiger Aufwand
  gehört in den Vorfilter (Embedding-Vergleich #543, härtere/echte Kataloge), nicht in ein
  teureres Auswahl-Modell.

### 10e. Grenzen (offen benennen)
- Die **Auswahl ist die leichte Hälfte** — der Vorfilter sortiert 400 → 15 (Gold immer dabei).
- **Sauberer synthetischer Katalog + von uns designte Fallen**; ein echter DATANORM-Katalog
  (kryptische Namen, dünne Beschreibungen) fordert Retrieval *und* Auswahl stärker.
- 16 Szenarien sind indikativ, keine Produktionsgarantie. Embedding-Vergleich (#543) steht aus.
