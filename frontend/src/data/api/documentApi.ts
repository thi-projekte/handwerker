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
    id: number;
    rechnungsnummer: string;
    offerBusinessKey: string;

    gesamtPreis: number;

    createdAt: string;
    updatedAt: string;

    kundendaten: {
        vorname: string;
        nachname: string;
        email: string;
        strasse: string;
        hausnummer: string;
        plz: string;
        ort: string;
    };
}

export type DocumentResponse = AngebotDTO[];
//   rechnungen: RechnungDTO[];

export interface DocumentMetadata {
    id: string; // documentId (UUID)
    offerId: number;
    fileName: string;
    createdAt: string;
    documentType: "ANGEBOT" | "RECHNUNG";
}

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

export const generateInvoiceDocument = async (
    invoiceId: string,
): Promise<DocumentMetadata | null> => {
    const token = await getToken();

    const res = await fetch(
        `https://craftvoice-document.winfprojekt.de/documents/invoices/${invoiceId}/generate`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...(token && { Authorization: `Bearer ${token}` }),
            },
        },
    );

    if (res.status === 404) return null;

    if (!res.ok) {
        throw new Error(`Failed to generate invoice document: ${res.status}`);
    }

    return res.json();
};
export const getPdfDownloadUrl = (documentId: string): string => {
    return `https://craftvoice-document.winfprojekt.de/documents/${documentId}/pdf`;
};
export const openDocumentPdfRechnung = async (invoiceId: string) => {
    const doc = await generateInvoiceDocument(invoiceId);

    if (!doc) {
        alert("Rechnung wird noch erstellt");
        return;
    }

    window.open(
        `https://craftvoice-document.winfprojekt.de/documents/${doc.id}/pdf`,
        "_blank",
    );
};