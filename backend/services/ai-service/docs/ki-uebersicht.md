# KI im CraftVoice-Projekt — Übersicht

**Zweck:** Eine verständliche Erklärung, **wann die KI**, **wann reine Programmierung** und
**wann die Datenbank** zum Einsatz kommt — für Pitch & Team. Oben die Übersicht auf einen
Blick, darunter die Details.

---

## 🧭 Auf einen Blick

### Die drei Bausteine — wer macht was?

| | Baustein | Aufgabe | Beispiel |
|---|---|---|---|
| 🧠 | **KI (LLM)** | **versteht & übersetzt** Sprache; matcht **Bedeutung** | „drei Doppelsteckdosen" → strukturierte Position; „Marmoroptik" → richtiges Produkt |
| ⚙️ | **Programmierung** | **sucht, rechnet, wendet feste Regeln an** | Katalog-Suche (Ranking), Preise rechnen, Summen, Mengen-Handling |
| 🗄️ | **Datenbank** | **speichert & liefert** Daten (inkl. **Preise**) | Katalogprodukte, Preise, Stundensatz, das Angebot selbst |

> **Merksatz:** Die **KI versteht**, die **Programmierung rechnet**, die **DB speichert**.
> Und die **goldene Regel: die KI sieht NIEMALS Preise** — die kommen erst danach aus der DB dazu.

### Der Sprach-Ablauf (Erstangebot) — Schritt für Schritt

```
  SPRACHE (Audio)
     │   Speech-to-Text (Deepgram, im offer-service)        ⚙️ Programmierung
     ▼
  TEXT (Sprachschnipsel)
     │   CALL 1 — Extraktion                                🧠 KI
     ▼
  STRUKTURIERTE POSITIONEN   (Leistungen + Material, OHNE Preise)
     │   pro Material-Position:
     │     ├─ Vorfilter: Katalog-Suche (Top-15)             ⚙️ + 🗄️ Programmierung + DB
     │     └─ CALL 2 — Produktauswahl                       🧠 KI
     ▼
  POSITIONEN + katalogProduktId
     │   Preise ergänzen: Stückpreis (Katalog),             ⚙️ + 🗄️ Programmierung + DB
     │   Stunden × Stundensatz, Anfahrt, Summen
     ▼
  FERTIGES ANGEBOT
```

### Der UI-Ablauf (manuelle Korrektur) — **ganz ohne KI**

```
  HANDWERKER EDITIERT POSITION IN DER UI
     │   Material suchen / auswählen                        ⚙️ + 🗄️ Programmierung + DB
     │   Preise neu rechnen                                 ⚙️ + 🗄️ Programmierung + DB
     ▼
  AKTUALISIERTES ANGEBOT                                    (KEINE KI!)
```

### Wann was? (Kurzantwort)

- **KI** läuft **nur beim Diktieren** (Sprache → Angebot): genau **2 Aufrufe** (Call 1 + Call 2).
- **Programmierung + DB** machen den Rest: Suche, Preise, Rechnen — **und alle manuellen UI-Korrekturen** (da ist gar keine KI beteiligt).

---

## 1. Was macht die KI im Projekt?

Die KI ist der **Übersetzer** von unstrukturierter Sprache in strukturierte Angebotsdaten.
Sie hat genau **zwei Jobs**:

1. **Call 1 – Extraktion:** aus dem gesprochenen Text strukturierte Positionen bauen.
2. **Call 2 – Produktauswahl:** zu jeder Materialposition das passende Katalogprodukt wählen.

**Was die KI bewusst NICHT macht:**
- ❌ **kein** Speech-to-Text (macht Deepgram im offer-service),
- ❌ **keine** Preise sehen oder bestimmen (Datenschutz-Constraint),
- ❌ **kein** Rechnen (Summen, Stundensatz, Anfahrt → offer-service),
- ❌ **keine** eigene Datenbank (der ai-service ist stateless).

Modell: **`gemini-3-flash-preview`** für beide Calls (Ergebnis der Modell-Evaluation, günstig + schnell + treffsicher).

---

## 2. Call 1 — Extraktion (Sprache → Positionen) · 🧠 KI

**Eingang:** transkribierter `sprachschnipsel` + `vorlage` (Stammdaten/Leistungskatalog des Handwerkers).
**Ausgang:** strukturierte Positionen, **ohne Preise**.

1. Die Process Engine schickt Sprachschnipsel + Vorlage an den ai-service.
2. Die KI wandelt den Fließtext in **Leistungen** und **Materialien** um — je mit
   `bezeichnung`, `beschreibung`, `menge`, `einheit`.
3. Sie ergänzt fachlich zwingende Positionen desselben Gewerks (z. B. Dose/Leitung bei Elektro)
   und schreibt Rückfragen/Hinweise in `korrekturvorschlaege`.
4. **Mengen:** explizit genannte Mengen werden exakt übernommen; unbekannte → `null` + Hinweis
   (kein Raten).
5. **Arbeitsdauer:** nur wenn der Handwerker eine Dauer **ausspricht** („zwei Stunden"), wird
   `geschaetzteArbeitsdauerStunden` gesetzt — die KI **schätzt nicht selbst**.

> Ergebnis ist reine Struktur — **kein einziger Preis**.

---

## 3. Call 2 — Produktauswahl (Position → Katalogprodukt)

Hier arbeiten **Programmierung, DB und KI zusammen** — in zwei Schritten pro Materialposition:

**Schritt A — Vorfilter (Katalog-Suche) · ⚙️ Programmierung + 🗄️ DB**
- Eine **Volltextsuche** im Katalog (Postgres-Volltext bzw. BM25) liefert die **Top-15
  Kandidaten** zur Position. Das ist **keine KI** — reine Such-Mechanik.
- Die Kandidaten gehen **ohne Preise** weiter.

**Schritt B — Auswahl · 🧠 KI**
- Die KI bekommt die 15 Kandidaten (neutral sortiert, ohne Preise) und wählt **genau einen**
  — oder gibt **`KEIN_TREFFER`** zurück, wenn nichts wirklich passt.
- Ergebnis: die **`katalogProduktId`** wird an die Position gehängt.

**Wichtig:**
- Läuft **parallel** über alle Materialpositionen (sonst bei 40–60 Positionen zu langsam).
- Der eigentliche Hebel ist die **Suche** (Vorfilter), nicht das Modell — die KI macht nur die
  Bedeutungs-Feinauswahl (z. B. „Marmoroptik" ≠ „Echtmarmor").

**Danach (nicht mehr KI):** der offer-service holt über die `katalogProduktId` den **Preis**
aus dem Katalog und rechnet alles zusammen. ⚙️ + 🗄️

---

## 4. Korrektur über die UI — **ohne KI** · ⚙️ + 🗄️

Wenn der Handwerker ein fertiges Angebot **manuell in der UI** ändert (Position bearbeiten,
Menge ändern, Material tauschen, etwas ergänzen):

1. Er tippt/wählt direkt — **die KI ist nicht beteiligt.**
2. Tauscht/ergänzt er ein **Material**, hilft die **Katalog-Suche** (⚙️ + 🗄️), das passende
   Produkt aus der Liste zu finden — die Auswahl trifft **er selbst**.
3. Der offer-service **rechnet die Preise neu** (⚙️ + 🗄️).

> **Warum keine KI?** Beim manuellen Editieren gibt es nichts „zu verstehen" — der Mensch
> entscheidet. KI lohnt nur dort, wo unstrukturierte Sprache in Struktur übersetzt werden muss.

**Abgrenzung — es gibt zwei Korrektur-Wege:**
- **Korrektur per Sprache** (Korrekturschnipsel einsprechen) → läuft **mit KI** durch Call 1
  (Korrektur-Modus) und ggf. Call 2 — wie ein „Mini-Erstangebot" auf den bestehenden Stand.
- **Korrektur per UI** (manuelles Editieren, dieser Abschnitt) → **ohne KI.**

---

## 5. Wann KI / wann Programmierung / wann DB? (Zusammenfassung)

| Aufgabe | 🧠 KI | ⚙️ Prog. | 🗄️ DB | Preise im Spiel? |
|---|:---:|:---:|:---:|---|
| Sprache → Text (Deepgram) | | ✓ | | nein |
| **Call 1:** Text → Positionen | ✓ | | | **nein** |
| Vorfilter: Katalog-Suche (Top-15) | | ✓ | ✓ | nein (Preise rausgefiltert) |
| **Call 2:** Produkt wählen | ✓ | | | **nein** |
| Preis-Lookup + Rechnen (Angebot) | | ✓ | ✓ | **ja** (erst hier!) |
| Manuelle UI-Korrektur | | ✓ | ✓ | ja |

**In einem Satz:** Die **KI** kommt nur an den **2 Übersetzungs-Stellen** beim Diktieren zum
Einsatz; **Suchen, Rechnen und alle manuellen Änderungen** sind **Programmierung + DB**.

---

## 6. Datenschutz: Preise nie an die KI

Das ist eine harte Bedingung (Lieferanten-/Handwerker-Verträge). Umsetzung:
- Die KI bekommt in Call 1 **nur** den Sprachschnipsel + Vorlage (preisfrei).
- In Call 2 werden die Katalog-Kandidaten **vor** dem KI-Aufruf von Preisen **bereinigt**.
- Preise (`Stückpreis`, `Stunden × Stundensatz`, Anfahrt, Summen) kommen **erst danach** im
  offer-service aus der DB dazu — getrennt von der KI.

So bleibt sensibles Preiswissen außerhalb der KI — und das Diagramm-Statement
„Sensible Preisdaten werden nicht an die KI übergeben" ist technisch wirklich abgesichert.
