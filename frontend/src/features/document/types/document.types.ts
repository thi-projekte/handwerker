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
  status: AngebotStatus;
  betrag: number;
}

export interface Rechnung {
  id: string;
  rechnungsnummer: string;

  vorname: string;
  nachname: string;

  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;

  erstelldatum: string;
  faelligkeitsdatum: string;

  status: RechnungStatus;
  betrag: number;
}