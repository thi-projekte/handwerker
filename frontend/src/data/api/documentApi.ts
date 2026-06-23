import { API_CONFIG } from "@/config/api";
import { getToken } from "@/services/authService";

export interface AngebotDTO {
    id: string;
    angebotsnummer: string;
    vorname: string;
    nachname: string;
    strasse: string;
    hausnummer: string;
    plz: string;
    ort: string;
    datum: string;
    status: "Erstellt" | "Versendet" | "Angenommen" | "Abgelehnt";
    betrag: number;
}

export interface RechnungDTO {
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
    status:
        | "Erstellt"
        | "Versendet"
        | "Bezahlt"
        | "Im Zahlungsverzug";
    betrag: number;
}

const API_BASE_URL = API_CONFIG.OFFER_SERVICE_URL;

export const getAngebote = async (): Promise<AngebotDTO[]> => {
    const token = await getToken();

    const response = await fetch(`${API_BASE_URL}/angebote`, {
        method: "GET",
        headers: {
            ...(token && {
                Authorization: `Bearer ${token}`,
            }),
        },
    });

    if (!response.ok) {
        throw new Error("Angebote konnten nicht geladen werden");
    }

    return response.json();
};

export const getRechnungen = async (): Promise<RechnungDTO[]> => {
    const token = await getToken();

    const response = await fetch(`${API_BASE_URL}/rechnungen`, {
        method: "GET",
        headers: {
            ...(token && {
                Authorization: `Bearer ${token}`,
            }),
        },
    });

    if (!response.ok) {
        throw new Error("Rechnungen konnten nicht geladen werden");
    }

    return response.json();
};