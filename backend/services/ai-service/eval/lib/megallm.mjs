// MegaLLM-Client + robustes Parsen der Modell-Antworten.
// Bewusst ohne externe Dependencies (Node 18+ fetch reicht).

const BASE_URL = process.env.MEGALLM_API_URL || "https://ai.megallm.io/v1";

/**
 * Striktes JSON-Schema fuer die Ausgabe (= unser ErgebnisKi-Vertrag).
 * Wird best-effort als response_format mitgeschickt. ACHTUNG: MegaLLM erzwingt
 * das NICHT bei allen Modellen (Smoke-Test 2026-05-31: gpt-5.4 ignorierte es).
 * Deshalb ist das robuste Parsen unten die eigentliche Absicherung.
 */
export const ERGEBNIS_SCHEMA = {
  type: "object",
  properties: {
    strukturierteAngebotspositionen: {
      type: "object",
      properties: {
        leistungen: { type: "array", items: positionSchema() },
        material:   { type: "array", items: positionSchema() },
        notizen:    { type: "array", items: { type: "string" } },
      },
      required: ["leistungen", "material", "notizen"],
      additionalProperties: false,
    },
    korrekturvorschlaege: { type: "array", items: { type: "string" } },
  },
  required: ["strukturierteAngebotspositionen", "korrekturvorschlaege"],
  additionalProperties: false,
};

function positionSchema() {
  return {
    type: "object",
    properties: {
      bezeichnung:  { type: "string" },
      beschreibung: { type: "string" },
      menge:        { type: ["number", "null"] },
      einheit:      { type: "string" },
    },
    required: ["bezeichnung", "beschreibung", "menge", "einheit"],
    additionalProperties: false,
  };
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** Ein einzelner HTTP-Versuch. Gibt {ok, status, text, latencyMs} bzw. {networkError}. */
async function singleAttempt({ apiKey, body, timeoutMs }) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const startedAt = Date.now();
  try {
    const res = await fetch(`${BASE_URL}/chat/completions`, {
      method: "POST",
      headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    const text = await res.text();
    return { ok: res.ok, status: res.status, text, latencyMs: Date.now() - startedAt };
  } catch (e) {
    const msg = e.name === "AbortError" ? `Timeout nach ${timeoutMs}ms` : String(e);
    return { networkError: msg, latencyMs: Date.now() - startedAt };
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Chat-Completion-Aufruf an MegaLLM mit ADAPTIVEM FALLBACK.
 * Wirft NICHT — Fehler werden als { error } zurueckgegeben.
 *
 * Selbstheilung (im Dry-Run 2026-05-31 als noetig entdeckt):
 *  - 400 "temperature ... deprecated"  -> retry OHNE temperature (z.B. claude-opus-4-8)
 *  - 400 Schema/Proto-Fehler           -> retry OHNE response_format (z.B. gemini-flash);
 *                                         robustes Parsen faengt das Schema dann ab
 *  - 503 / 429 / 5xx / Timeout         -> retry mit Backoff (2s,4s,8s)
 *
 * Im Ergebnis stehen strictUsed/temperatureUsed -> selbst ein Schema-Datenpunkt:
 * ein Modell, das response_format gar nicht akzeptiert, ist beim Schema-Kriterium
 * schwaecher als eins, das es akzeptiert UND befolgt.
 */
export async function callModel({ model, systemPrompt, userContent, temperature = 0.2, strict = true, timeoutMs = 90000, maxTransientRetries = 3 }) {
  const apiKey = process.env.MEGALLM_API_KEY;
  if (!apiKey) throw new Error("MEGALLM_API_KEY ist nicht gesetzt.");

  let useTemp = temperature;
  let useStrict = strict;
  let transientTries = 0;
  let totalLatency = 0;

  while (true) {
    const body = {
      model,
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: userContent },
      ],
    };
    if (useTemp !== null && useTemp !== undefined) body.temperature = useTemp;
    if (useStrict) {
      body.response_format = {
        type: "json_schema",
        json_schema: { name: "ergebnis_ki", strict: true, schema: ERGEBNIS_SCHEMA },
      };
    }

    const attempt = await singleAttempt({ apiKey, body, timeoutMs });
    totalLatency += attempt.latencyMs;

    // Netzwerk/Timeout -> transient retry
    if (attempt.networkError) {
      if (transientTries < maxTransientRetries) {
        transientTries++;
        await sleep(2000 * 2 ** (transientTries - 1));
        continue;
      }
      return { error: attempt.networkError, latencyMs: totalLatency };
    }

    if (attempt.ok) {
      let json;
      try { json = JSON.parse(attempt.text); }
      catch { return { error: `Antwort kein JSON: ${attempt.text.slice(0, 200)}`, latencyMs: totalLatency }; }
      const usage = json?.usage ?? {};
      return {
        rawOutput: json?.choices?.[0]?.message?.content ?? "",
        latencyMs: attempt.latencyMs,
        promptTokens: usage.prompt_tokens ?? null,
        completionTokens: usage.completion_tokens ?? null,
        totalTokens: usage.total_tokens ?? null,
        strictUsed: useStrict,
        temperatureUsed: useTemp !== null && useTemp !== undefined,
      };
    }

    // --- Fehlerbehandlung mit Fallbacks ---
    const msg = (attempt.text || "").toLowerCase();

    if (attempt.status === 400 && msg.includes("temperature") && useTemp !== null && useTemp !== undefined) {
      useTemp = null; // Feld entfernen und erneut versuchen
      continue;
    }
    if (attempt.status === 400 && useStrict &&
        (msg.includes("response_schema") || msg.includes("response_format") ||
         msg.includes("json_schema") || msg.includes("proto field") || msg.includes("schema"))) {
      useStrict = false; // ohne striktes Schema, robustes Parsen uebernimmt
      continue;
    }
    // transiente HTTP-Fehler
    if ((attempt.status === 503 || attempt.status === 429 || attempt.status >= 500) &&
        transientTries < maxTransientRetries) {
      transientTries++;
      await sleep(2000 * 2 ** (transientTries - 1));
      continue;
    }

    return { error: `HTTP ${attempt.status}: ${attempt.text.slice(0, 300)}`, latencyMs: totalLatency };
  }
}

/**
 * Robustes Parsen des Modell-Outputs zu unserem ErgebnisKi-Objekt.
 * Toleriert: Markdown-Fences (```json ... ```), den fehlenden Wrapper
 * (manche Modelle geben {leistungen,material,notizen} direkt zurueck),
 * und fehlendes korrekturvorschlaege.
 *
 * Gibt { ok, value, schemaTreue, probleme } zurueck:
 *  - ok: konnte ueberhaupt zu unserem Schema normalisiert werden
 *  - schemaTreue: 'exakt' (sauberer Wrapper) | 'fences' | 'kein-wrapper' | 'kaputt'
 *  - probleme: Liste der Abweichungen (fuer das Schema-Konformitaets-Kriterium)
 */
export function parseErgebnis(rawOutput) {
  const probleme = [];
  let text = (rawOutput ?? "").trim();

  // 1) Markdown-Fences entfernen
  const hadFences = /^```/.test(text);
  if (hadFences) {
    probleme.push("markdown-fences");
    text = text.replace(/^\s*```(?:json)?\s*/i, "").replace(/\s*```\s*$/i, "").trim();
  }

  // 2) JSON parsen
  let obj;
  try {
    obj = JSON.parse(text);
  } catch {
    return { ok: false, value: null, schemaTreue: "kaputt", probleme: [...probleme, "kein-valides-json"] };
  }

  // 3) Wrapper normalisieren
  let wrapped = obj;
  let hadWrapper = true;
  if (!obj.strukturierteAngebotspositionen && (obj.leistungen || obj.material || obj.notizen)) {
    // Modell gab den inneren Block direkt zurueck -> einwickeln
    hadWrapper = false;
    probleme.push("kein-wrapper");
    wrapped = {
      strukturierteAngebotspositionen: {
        leistungen: obj.leistungen ?? [],
        material: obj.material ?? [],
        notizen: obj.notizen ?? [],
      },
      korrekturvorschlaege: obj.korrekturvorschlaege ?? [],
    };
  }

  const sap = wrapped.strukturierteAngebotspositionen ?? {};
  if (!Array.isArray(sap.leistungen)) { sap.leistungen = []; probleme.push("leistungen-fehlt"); }
  if (!Array.isArray(sap.material))   { sap.material = [];   probleme.push("material-fehlt"); }
  if (!Array.isArray(sap.notizen))    { sap.notizen = [];    probleme.push("notizen-fehlt"); }
  if (!Array.isArray(wrapped.korrekturvorschlaege)) {
    wrapped.korrekturvorschlaege = [];
    probleme.push("korrekturvorschlaege-fehlt");
  }
  wrapped.strukturierteAngebotspositionen = sap;

  let schemaTreue = "exakt";
  if (!hadWrapper) schemaTreue = "kein-wrapper";
  else if (hadFences) schemaTreue = "fences";

  return { ok: true, value: wrapped, schemaTreue, probleme };
}

/** USD-Kosten eines Aufrufs aus Tokens + Modell-Preisen (USD pro 1M). */
export function berechneKosten(promptTokens, completionTokens, preisInput, preisOutput) {
  const pt = promptTokens ?? 0;
  const ct = completionTokens ?? 0;
  return (pt * preisInput + ct * preisOutput) / 1_000_000;
}
