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
 *   POST /angebote/{businessKey}/arbeitsstunden → manuelle Arbeitsstunden-Eingabe
 *   [TODO] POST /angebote/{businessKey}/korrektur → Fall 3: Korrekturschnipsel
 *         → Endpunkt noch nicht implementiert; sendKorrektur wirft daher.
 *
 * ⚠️  Was das Frontend vom AI-Service NICHT direkt bekommt:
 *   - KI wird von der PE getriggert, nicht vom Frontend
 *   - Preise kommen nie vom AI-Service (Datenschutz)
 *   - Frontend pollt GET /offers/{businessKey} bis status === "KI_FERTIG"
 */

import { API_CONFIG } from "@/config/api";
import { getToken } from "@/services/authService";

// ─── Auth-Helper ────────────────────────────────────────────────────────────

async function getAuthHeaders(): Promise<Record<string, string>> {
  const token = await getToken();
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
  // Feldname und Werte exakt wie im Backend (Entity OfferPosition.type,
  // Enum common.OfferPositionType). Es gibt KEIN "LEISTUNG".
  type: "MATERIAL" | "ARBEITSZEIT" | "ANFAHRT";
  katalogProduktId: string | null; // UUID-String, kein Long!
}

export interface OfferResponse {
  id: number;
  businessKey: string;
  // String, passend zum Backend-DTO (OfferResponse.customerId/handwerkerId).
  // handwerkerId ist die Keycloak-UUID (sub), keine Zahl.
  customerId: string;
  handwerkerId: string;
  status: string;
  gesamtPreis: number | null;
  korrekturvorschlaege: string[];
  // Von der KI geschätzte Arbeitsdauer in Stunden; null, wenn der Handwerker im
  // Sprachschnipsel keine Dauer ausgesprochen hat (dann manuelle Pflichteingabe).
  geschaetzteArbeitsdauerStunden: number | null;
  positions: OfferPosition[];
  createdAt: string;
}

export interface CreateOfferRequest {
  // String, passend zum Backend-DTO (CreateOfferRequest.customerId).
  customerId: string;
  // handwerkerId wird backendseitig aus dem JWT abgeleitet, nicht mitgesendet.
  speechSnippet: string;
}

/**
 * Eine strukturierte Position wie im Backend-DTO StructuredOfferPositionDTO.
 * Enthält bewusst KEINEN Preis — Preise kommen aus dem catalog-service.
 */
export interface StructuredOfferPosition {
  bezeichnung: string;
  hersteller?: string | null;
  beschreibung: string;
  menge: number | null;
  einheit: string;
  katalogProduktId?: string | null;
}

/**
 * Passt zum Backend-DTO OfferChangesRequest: strukturierteAngebotspositionen ist
 * ein Objekt { leistungen, material, notizen } (KI-Struktur). Verarbeitet wird
 * backendseitig nur `material`. `korrekturvorschlaege` ist Pflicht (ggf. []).
 */
export interface OfferChangesRequest {
  strukturierteAngebotspositionen: {
    leistungen: StructuredOfferPosition[];
    material: StructuredOfferPosition[];
    notizen: string[];
  };
  korrekturvorschlaege: string[];
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
    headers: await getAuthHeaders(),
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
    { headers: await getAuthHeaders() },
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
      headers: await getAuthHeaders(),
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
      headers: await getAuthHeaders(),
      body: JSON.stringify(request),
    },
  );

  if (!res.ok) {
    throw new Error(`Positions-Update fehlgeschlagen: ${res.status}`);
  }
}

/**
 * Manuelle Arbeitsstunden-Eingabe durch den Handwerker.
 * Der Handwerker trägt die Arbeitsdauer selbst ein (kommt NICHT von der KI);
 * der offer-service berechnet daraus die ARBEITSZEIT-Position neu.
 *
 * Backend: POST /angebote/{businessKey}/arbeitsstunden (SetArbeitsstundenRequest).
 */
export interface SetArbeitsstundenRequest {
  // Entspricht SetArbeitsstundenRequest.arbeitsdauerStunden (BigDecimal, >= 0).
  arbeitsdauerStunden: number;
}

export async function setArbeitsstunden(
  businessKey: string,
  request: SetArbeitsstundenRequest,
): Promise<OfferResponse> {
  const res = await fetch(
    `${API_CONFIG.OFFER_SERVICE_URL}/angebote/${businessKey}/arbeitsstunden`,
    {
      method: "POST",
      headers: await getAuthHeaders(),
      body: JSON.stringify(request),
    },
  );

  if (!res.ok) {
    throw new Error(`Arbeitsstunden setzen fehlgeschlagen: ${res.status}`);
  }

  return res.json();
}

/**
 * Fall 3: Handwerker hat manuelle Änderungen vorgenommen.
 *
 * TODO: Dieser Endpunkt existiert noch nicht im offer-service.
 * Erwarteter Endpunkt: POST /angebote/{businessKey}/korrektur
 *
 * Solange der Endpunkt fehlt, wirft diese Funktion bewusst einen Fehler,
 * statt Erfolg vorzutäuschen — der Aufrufer muss den Fehlerfall behandeln.
 */
export async function sendKorrektur(
  businessKey: string,
  korrekturschnipsel: string,
): Promise<void> {
  void korrekturschnipsel;
  throw new Error(
    `Korrektur-Endpunkt im offer-service noch nicht implementiert (businessKey ${businessKey}).`,
  );
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
