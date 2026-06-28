/**
 * processEngineService.ts
 *
 * Direktkommunikation zwischen Frontend und Camunda Process Engine.
 *
 * Laut Architektur sendet das Frontend bestimmte PE-Nachrichten direkt
 * (nicht über den offer-service), weil sie "Frontend-Trigger" im BPMN sind:
 *
 *   - "genehmigungAngebot"   → Fall 1: Handwerker genehmigt (keine Änderungen)
 *   - "angebotsentwurf"      → Fall 2: Handwerker wählt Alternative / ändert Reihenfolge
 *   - "korrekturschnipsel"   → Fall 3: Manuelle Änderung → löst neuen KI-Durchlauf aus
 *   - "auftragVersenden"     → Auf OfferSharePage: Handwerker sendet Angebot ab
 *   - "auftragNichtVersenden"→ Auf OfferSharePage: Handwerker verwirft
 *
 * ⚠️  PE-URL: pe-craftvoice.winfprojekt.de (Portainer-Stack: craftvoice-processengine)
 */

import { API_CONFIG } from "@/config/api";
import { getToken } from "@/services/authService";
import type {
  OfferChangesRequest,
  OfferResponse,
} from "@/data/api/offerService";

/**
 * Payload der "angebotsentwurf"-Nachricht im Fall 2.
 *
 * Bewusst ein **kombiniertes** Format: Es enthält gleichzeitig
 *   - die OfferResponse-Felder (positions, gesamtPreis, businessKey, customerId,
 *     createdAt …) → der document-service/PdfGenerator rendert daraus die
 *     Positionstabelle inkl. Preise, und
 *   - strukturierteAngebotspositionen + korrekturvorschlaege → der PE-Task
 *     "Angebotsentwurf aktualisieren" (Activity_4.4) reicht genau diese Felder
 *     unverändert an /positionen weiter.
 *
 * Beide Consumer ignorieren die jeweils fremden Felder (Jackson auf der
 * Backend-Seite verwirft unbekannte Properties), sodass ein einziges Objekt
 * beide Schritte bedient.
 */
export type AngebotsentwurfPayload = OfferChangesRequest & Partial<OfferResponse>;

// ─── Auth-Helper ────────────────────────────────────────────────────────────

async function getAuthHeaders(): Promise<Record<string, string>> {
  const token = await getToken();
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

// ─── Typen ───────────────────────────────────────────────────────────────────

interface PeProcessVariable {
  value: unknown;
  type: string;
}

interface PeMessagePayload {
  messageName: string;
  businessKey: string;
  processVariables?: Record<string, PeProcessVariable>;
  resultEnabled?: boolean;
}

// ─── Kern-Funktion ───────────────────────────────────────────────────────────

async function sendPeMessage(payload: PeMessagePayload): Promise<void> {
  const res = await fetch(`${API_CONFIG.PE_URL}/message`, {
    method: "POST",
    headers: await getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(
      `PE-Nachricht "${payload.messageName}" fehlgeschlagen (${res.status}): ${body}`,
    );
  }
}

// ─── Fall 1: Genehmigung (keine Änderungen) ─────────────────────────────────

/**
 * Sendet "genehmigungAngebot" direkt an die PE.
 * Der offer-service wurde bereits über approveOffer() informiert.
 * Die PE leitet danach den Versandprozess ein (Dokument-Erstellung).
 */
export async function sendGenehmigung(businessKey: string): Promise<void> {
  await sendPeMessage({
    messageName: "genehmigungAngebot",
    businessKey,
    resultEnabled: false,
  });
}

// ─── Fall 2: Reihenfolge / Alternative geändert ──────────────────────────────

/**
 * Sendet "angebotsentwurf" direkt an die PE (Fall 2: Reihenfolge / Alternative).
 *
 * Der Payload ist das {@link AngebotsentwurfPayload}-Kombiformat: Es enthält
 * sowohl {@code strukturierteAngebotspositionen.material} (Bezeichnung + Menge +
 * Einheit + katalogProduktId der gewählten Alternative — für den PE-/positionen-
 * Schritt) als auch die OfferResponse-Felder mit {@code positions} und
 * {@code gesamtPreis} (für den document-service, der die PDF-Positionstabelle
 * ausschließlich aus {@code positions} aufbaut). Ohne die OfferResponse-Felder
 * bliebe die Positionstabelle im PDF leer.
 *
 * Serialisierung: processVariables.angebotsentwurf = { value: JSON-String,
 * type: "Json" }.
 */
export async function sendAngebotsentwurf(
  businessKey: string,
  angebotsentwurf: AngebotsentwurfPayload,
): Promise<void> {
  await sendPeMessage({
    messageName: "angebotsentwurf",
    businessKey,
    processVariables: {
      angebotsentwurf: {
        value: JSON.stringify(angebotsentwurf),
        type: "Json",
      },
    },
    resultEnabled: false,
  });
}

// ─── Fall 3: Manuelle Korrektur ──────────────────────────────────────────────

/**
 * Sendet "korrekturschnipsel" direkt an die PE (Fall 3).
 * Die PE triggert daraufhin einen neuen KI-Durchlauf.
 * Das Frontend landet wieder auf /laden → /review.
 */
export async function sendKorrekturschnipsel(
  businessKey: string,
  korrekturschnipsel: string,
): Promise<void> {
  await sendPeMessage({
    messageName: "korrekturschnipsel",
    businessKey,
    processVariables: {
      korrekturschnipsel: {
        value: korrekturschnipsel,
        type: "String",
      },
    },
    resultEnabled: false,
  });
}

// ─── Versand-Entscheidungen (OfferSharePage) ─────────────────────────────────

/**
 * Handwerker möchte das Angebot versenden.
 * PE leitet den Angebotsversand ein.
 */
export async function sendAuftragVersenden(businessKey: string): Promise<void> {
  await sendPeMessage({
    messageName: "angebotVersenden",
    businessKey,
    resultEnabled: false,
  });
}

/**
 * Handwerker möchte das Angebot NICHT versenden.
 * PE beendet den Versandprozess ohne Versand.
 */
export async function sendAuftragNichtVersenden(
  businessKey: string,
): Promise<void> {
  await sendPeMessage({
    messageName: "angebotNichtVersenden",
    businessKey,
    resultEnabled: false,
  });
}
