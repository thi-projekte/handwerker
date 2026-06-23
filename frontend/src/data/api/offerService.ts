/**
 * offerService.ts
 *
 * Client für den CraftVoice offer-service.
 * Das Frontend kommuniziert NICHT direkt mit dem ai-service —
 * KI-Ergebnisse kommen ausschließlich über diesen Service (via PE).
 *
 * Bekannte Endpunkte:
 *   POST /offers                              → Angebot erstellen (startet PE-Flow)
 *   GET  /offers/{businessKey}               → Angebot + Positionen abrufen
 *   POST /offers/{businessKey}/review/approve → Fall 1: Genehmigung
 *   POST /angebote/{businessKey}/positionen   → Fall 2: Positionen-Update (Reihenfolge/Alternative)
 *   [TODO] POST /angebote/{businessKey}/korrektur → Fall 3: Korrekturschnipsel
 *         → Endpunkt noch nicht implementiert, wird hier als Vorbereitung angelegt.
 *
 * ⚠️  Was das Frontend vom AI-Service NICHT direkt bekommt:
 *   - KI wird von der PE getriggert, nicht vom Frontend
 *   - Preise kommen nie vom AI-Service (Datenschutz)
 *   - Frontend pollt GET /offers/{businessKey} bis status === "KI_FERTIG"
 */

import { API_CONFIG } from "@/config/api";

// ─── Auth-Helper ────────────────────────────────────────────────────────────

function getAuthHeaders(): Record<string, string> {
  const token = localStorage.getItem("authToken");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

// ─── Typen ───────────────────────────────────────────────────────────────────

export interface OfferPosition {
  id: number;
  bezeichnung: string;
  beschreibung: string;
  menge: number | null;
  einheit: string;
  einzelPreis: number | null;
  positionsPreis: number | null;
  typ: "LEISTUNG" | "MATERIAL" | "ARBEITSZEIT" | "ANFAHRT";
  katalogProduktId: string | null; // UUID-String, kein Long!
}

export interface OfferResponse {
  id: number;
  businessKey: string;
  customerId: number;
  handwerkerId: number;
  status: string;
  gesamtPreis: number | null;
  korrekturvorschlaege: string[];
  geschaetzteArbeitsdauerStunden: number | null;
  positions: OfferPosition[];
  createdAt: string;
}

export interface CreateOfferRequest {
  customerId: number;
  handwerkerId: number;
  speechSnippet: string;
}

export interface OfferChangesRequest {
  korrekturvorschlaege?: string[];
  geschaetzteArbeitsdauerStunden?: number | null;
  positionen: {
    bezeichnung: string;
    beschreibung: string;
    menge: number | null;
    einheit: string;
    einzelPreis?: number | null;
    typ: string;
    katalogProduktId?: string | null;
  }[];
}

// ─── API-Funktionen ──────────────────────────────────────────────────────────

/**
 * Erstellt ein neues Angebot im offer-service.
 * Der offer-service startet daraufhin den PE-Prozess (angebotPayload).
 * Gibt den businessKey zurück, der für alle weiteren Aufrufe benötigt wird.
 */
export async function createOffer(
  request: CreateOfferRequest,
): Promise<OfferResponse> {
  const res = await fetch(`${API_CONFIG.OFFER_SERVICE_URL}/offers`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!res.ok) {
    throw new Error(`Angebot erstellen fehlgeschlagen: ${res.status}`);
  }

  return res.json();
}

/**
 * Ruft ein Angebot anhand des businessKey ab.
 * Wird für Polling verwendet: Frontend fragt ab, bis status === "KI_FERTIG".
 *
 * Das Ergebnis enthält die Positionen aus dem KI-Durchlauf (leistungen/material
 * wurden vom offer-service bereits mit Preisen aus dem catalog-service angereichert).
 */
export async function getOfferByBusinessKey(
  businessKey: string,
): Promise<OfferResponse> {
  const res = await fetch(
    `${API_CONFIG.OFFER_SERVICE_URL}/offers/${businessKey}`,
    { headers: getAuthHeaders() },
  );

  if (!res.ok) {
    throw new Error(`Angebot nicht gefunden: ${res.status}`);
  }

  return res.json();
}

/**
 * Fall 1: Handwerker ist zufrieden, keine Änderungen.
 * Setzt Status auf KI_BEARBEITUNG_ABGESCHLOSSEN im offer-service.
 * Die PE-Nachricht "genehmigungAngebot" wird danach separat direkt an die PE gesendet.
 */
export async function approveOffer(businessKey: string): Promise<void> {
  const res = await fetch(
    `${API_CONFIG.OFFER_SERVICE_URL}/offers/${businessKey}/review/approve`,
    {
      method: "POST",
      headers: getAuthHeaders(),
    },
  );

  if (!res.ok) {
    throw new Error(`Genehmigung fehlgeschlagen: ${res.status}`);
  }
}

/**
 * Fall 2: Handwerker hat Reihenfolge geändert oder Alternative gewählt.
 * Aktualisiert die Positionen im offer-service.
 * Die PE-Nachricht "angebotsentwurf" wird danach separat direkt an die PE gesendet.
 */
export async function updateOfferPositions(
  businessKey: string,
  request: OfferChangesRequest,
): Promise<void> {
  const res = await fetch(
    `${API_CONFIG.OFFER_SERVICE_URL}/angebote/${businessKey}/positionen`,
    {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(request),
    },
  );

  if (!res.ok) {
    throw new Error(`Positions-Update fehlgeschlagen: ${res.status}`);
  }
}

/**
 * Fall 3: Handwerker hat manuelle Änderungen vorgenommen.
 *
 * TODO: Dieser Endpunkt existiert noch nicht im offer-service.
 * Vorbereitung: Sobald Lennart/Marvin den Endpunkt implementiert haben,
 * diese Funktion aktivieren. Aktuell wird nur die PE-Nachricht gesendet.
 *
 * Erwarteter Endpunkt: POST /angebote/{businessKey}/korrektur
 */
export async function sendKorrektur(
  businessKey: string,
  korrekturschnipsel: string,
): Promise<void> {
  // TODO: Endpunkt noch nicht vorhanden — wird vorbereitet
  console.warn(
    "[offerService] sendKorrektur: Endpunkt noch nicht implementiert.",
    { businessKey, korrekturschnipsel },
  );

  // Wenn der Endpunkt verfügbar ist, so aufrufen:
  // const res = await fetch(
  //   `${API_CONFIG.OFFER_SERVICE_URL}/angebote/${businessKey}/korrektur`,
  //   {
  //     method: "POST",
  //     headers: getAuthHeaders(),
  //     body: JSON.stringify({ korrekturschnipsel }),
  //   },
  // );
  // if (!res.ok) throw new Error(`Korrektur fehlgeschlagen: ${res.status}`);
}

/**
 * Polling-Hilfsfunktion: Wartet, bis das Angebot den Status "KI_FERTIG" hat.
 *
 * @param businessKey  businessKey des Angebots
 * @param intervalMs   Polling-Intervall in ms (default: 2500)
 * @param timeoutMs    Maximale Wartezeit in ms (default: 120000 = 2 min)
 * @param onStatus     Optional: Callback bei jedem Poll mit aktuellem Status
 */
export async function pollUntilKiFertig(
  businessKey: string,
  intervalMs = 2500,
  timeoutMs = 120_000,
  onStatus?: (status: string) => void,
): Promise<OfferResponse> {
  const deadline = Date.now() + timeoutMs;

  while (Date.now() < deadline) {
    const offer = await getOfferByBusinessKey(businessKey);
    onStatus?.(offer.status);

    if (offer.status === "KI_FERTIG") {
      return offer;
    }

    // Warte vor nächstem Poll
    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }

  throw new Error(
    `Timeout: KI-Ergebnis für ${businessKey} nicht innerhalb von ${timeoutMs / 1000}s verfügbar.`,
  );
}
