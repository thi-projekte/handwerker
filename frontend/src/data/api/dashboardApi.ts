import { getToken } from "@/services/authService";

export interface ChartDataDTO {
  month: string;
  angebote: number;
}

export interface AktivitaetDTO {
  offerId: number;
  businessKey: string;
  customerId: number;
  status: string;
  zeitpunkt: string;
}

export interface AufmerksamkeitDTO {
  offerId: number;
  businessKey: string;
  customerId: number;
  versendetAm: string;
}

export interface DashboardStatsResponse {
  angeboteGesamt: number;
  ohneRueckmeldung: number;
  mitRueckmeldung: number;
  nichtFertiggestellt: number;
  rechnungenAusgestellt: number;
  rechnungenBezahlt: number;
  rechnungsvolumen: number;
  letzteAktivitaeten: AktivitaetDTO[];
  aufmerksamkeitErforderlich: AufmerksamkeitDTO[];
  angebotsuebersicht: ChartDataDTO[];
}

const API_BASE_URL = import.meta.env.VITE_OFFER_SERVICE_URL || "http://offerservice-craftvoice.winfprojekt.de";

export const getDashboardStats = async (): Promise<DashboardStatsResponse> => {
  const token = await getToken();
  
  const response = await fetch(`${API_BASE_URL}/offers`, {
    method: "GET",
    headers: {
      ...(token && { Authorization: `Bearer ${token}` }),
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch dashboard stats: ${response.statusText}`);
  }

  return response.json();
};
