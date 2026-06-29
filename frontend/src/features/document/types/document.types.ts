export type AngebotStatus =
  | "Erstellt"
  | "Versendet"
  | "Angenommen"
  | "Abgelehnt";

export type RechnungStatus =
  | "Erstellt"
  | "Versendet"
  | "Bezahlt"
  | "Im Zahlungsverzug";

export interface Angebot {
  id: string;
  angebotsnummer: string;

  vorname: string;
  nachname: string;

  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;

  datum: string;
  /** Volle ISO-Datetime der Erstellung (für die Uhrzeit-Anzeige). */
  erstelltAm: string;
  status: AngebotStatus;
  betrag: number;
}

export interface Rechnung {
  id: string;
  rechnungsnummer: string;
  offerBusinessKey: string;

  vorname: string;
  nachname: string;

  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;

  erstelldatum: string;
  faelligkeitsdatum: string;
  /** Volle ISO-Datetime der Erstellung (für die Uhrzeit-Anzeige). */
  erstelltAm: string;

  status: RechnungStatus;
  betrag: number;
}