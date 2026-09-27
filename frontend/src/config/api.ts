/**
 * Zentrale API-Konfiguration für alle CraftVoice-Services.
 * URLs können über Umgebungsvariablen überschrieben werden.
 */

export const API_CONFIG = {
  OFFER_SERVICE_URL:
    import.meta.env.VITE_OFFER_SERVICE_URL ??
    "http://craftvoice-offer.winfprojekt.de",
  USER_SERVICE_URL:
    import.meta.env.VITE_USER_SERVICE_URL ??
    "http://craftvoice-user.winfprojekt.de",

  DOCUMENT_SERVICE_URL:
    import.meta.env.VITE_DOCUMENT_SERVICE_URL ??
    "http://craftvoice-document.winfprojekt.de",

  PE_URL:
    import.meta.env.VITE_PE_URL ??
    "http://pe-craftvoice.winfprojekt.de/engine-rest",

  CATALOG_SERVICE_URL:
    import.meta.env.VITE_CATALOG_SERVICE_URL ??
    "https://craftvoice-catalog.winfprojekt.de",
} as const;

/** Offer-Service Status-Konstanten (spiegeln das Backend wider) */
export const OFFER_STATUS = {
  IN_BEARBEITUNG: "IN_BEARBEITUNG",
  KI_FERTIG: "KI_FERTIG",
  KI_BEARBEITUNG_ABGESCHLOSSEN: "KI_BEARBEITUNG_ABGESCHLOSSEN",
  VERSANDBEREIT: "VERSANDBEREIT",
  VERSENDET: "VERSENDET",
  ANGENOMMEN: "ANGENOMMEN",
  ABGELEHNT: "ABGELEHNT",
} as const;

export type OfferStatus = (typeof OFFER_STATUS)[keyof typeof OFFER_STATUS];
