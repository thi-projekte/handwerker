/**
 * documentService.ts
 *
 * Client für den CraftVoice document-service.
 * Zuständig für PDF-Generierung, -Download und -Versand.
 *
 * Basis-URL: craftvoice-document.winfprojekt.de
 *
 * Endpunkte:
 *   POST /documents/offers/{offerId}/generate  → PDF erstellen (wird normalerweise von PE ausgelöst)
 *   GET  /documents/offers/{offerId}           → Dokument-Metadaten für ein Angebot
 *   GET  /documents/{documentId}/pdf           → PDF-Datei herunterladen
 *   POST /documents/offers/{offerId}/share     → PDF per E-Mail versenden
 *   POST /documents/invoices/{invoiceId}/share → Rechnung per E-Mail versenden
 *
 * Wichtig: offerId ist die numerische ID des Angebots aus dem offer-service,
 * NICHT der businessKey (das sind zwei verschiedene Dinge).
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

export interface DocumentMetadata {
  id: string; // documentId (UUID)
  offerId: number;
  fileName: string;
  createdAt: string;
  documentType: "ANGEBOT" | "RECHNUNG";
}

// ─── API-Funktionen ──────────────────────────────────────────────────────────

/**
 * Gibt die Download-URL für ein PDF zurück.
 * Die URL kann direkt in einem <a href> oder window.open() verwendet werden.
 *
 * @param documentId  UUID des Dokuments (aus DocumentMetadata.id)
 */
export function getPdfDownloadUrl(documentId: string): string {
  return `${API_CONFIG.DOCUMENT_SERVICE_URL}/documents/${documentId}/pdf`;
}

/**
 * Ruft die Dokument-Metadaten für ein Angebot ab.
 * Damit bekommt man die documentId, die für den PDF-Download benötigt wird.
 *
 * Hinweis: Wenn noch kein Dokument existiert, gibt der Service 404 zurück.
 * Das passiert, wenn die PE die PDF-Erstellung noch nicht abgeschlossen hat.
 *
 * @param offerId  Numerische ID des Angebots aus dem offer-service
 */
export async function getDocumentByOfferId(
  businessKey: string,
): Promise<DocumentMetadata | null> {
  const res = await fetch(
    `${API_CONFIG.DOCUMENT_SERVICE_URL}/documents/offers/${businessKey}`,
    { headers: await getAuthHeaders() },
  );

  if (res.status === 404) return null;

  if (!res.ok) {
    throw new Error(
      `Dokument-Metadaten konnten nicht geladen werden: ${res.status}`,
    );
  }

  return res.json();
}

/**
 * Sendet das Angebots-PDF per E-Mail an den Kunden.
 * Falls das Dokument noch nicht existiert, generiert es der Service automatisch.
 *
 * @param offerId  Numerische ID des Angebots aus dem offer-service
 */
export async function shareOfferByEmail(offerId: number): Promise<void> {
  const res = await fetch(
    `${API_CONFIG.DOCUMENT_SERVICE_URL}/documents/offers/${offerId}/share`,
    {
      method: "POST",
      headers: await getAuthHeaders(),
    },
  );

  if (!res.ok) {
    throw new Error(`E-Mail-Versand fehlgeschlagen: ${res.status}`);
  }
}

/**
 * Polling-Hilfsfunktion: Wartet, bis das PDF für ein Angebot verfügbar ist.
 * Die PE triggert die PDF-Erstellung — das Frontend pollt, bis das Dokument existiert.
 *
 * @param offerId     Numerische ID des Angebots
 * @param intervalMs  Polling-Intervall in ms (default: 2000)
 * @param timeoutMs   Maximale Wartezeit in ms (default: 60000 = 1 min)
 */
export async function pollUntilPdfReady(
  offerId: string,
  intervalMs = 2000,
  timeoutMs = 60_000,
): Promise<DocumentMetadata> {
  const deadline = Date.now() + timeoutMs;

  while (Date.now() < deadline) {
    const doc = await getDocumentByOfferId(offerId);
    if (doc) return doc;

    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }

  throw new Error(
    `Timeout: PDF für Angebot ${offerId} nicht innerhalb von ${timeoutMs / 1000}s verfügbar.`,
  );
}
