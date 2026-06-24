export type AngebotStatus =
  | "Erstellt"
  | "Versendet"
  | "Angenommen"
  | "Abgelehnt";

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