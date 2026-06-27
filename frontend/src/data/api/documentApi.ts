import { API_CONFIG } from "@/config/api";
import { getToken } from "@/services/authService";

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

export interface AngebotDTO {
  id: number;
  businessKey: string;
  status: string;
  customerId: string;
  handwerkerId: string;
  speechSnippet: string;
  createdAt: string;
  gesamtPreis: number | null;
}

export interface RechnungDTO {
    id: string;
    rechnungsnummer: string;
    customerId: string;
    erstelldatum: string;
    faelligkeitsdatum: string;
    status: string;
    betrag: number;
}

export type DocumentResponse = AngebotDTO[];
//   rechnungen: RechnungDTO[];


const API_BASE_URL = API_CONFIG.OFFER_SERVICE_URL;

export const getDocuments = async (): Promise<DocumentResponse> => {
    const token = await getToken();

    const response = await fetch(`${API_BASE_URL}/offers`, {
        method: "GET",
        headers: {
            ...(token && {
                Authorization: `Bearer ${token}`,
            }),
        },
    });

    if (!response.ok) {
        throw new Error(
            `Failed to fetch documents: ${response.status} ${response.statusText}`,
        );
    }

    return response.json();
};

export const getRechnungen = async (): Promise<RechnungDTO[]> => {
    const token = await getToken();

    const response = await fetch(
        "https://offerservice-craftvoice.winfprojekt.de/rechnungen",
        {
            method: "GET",
            headers: {
                ...(token && {
                    Authorization: `Bearer ${token}`,
                }),
            },
        },
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch invoices: ${response.status} ${response.statusText}`,
        );
    }

    return response.json();
};