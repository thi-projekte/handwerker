import type { Angebot, Rechnung, RechnungStatus } from "@/features/document/types/document.types";

interface OfferDTO {
    id: number | string;
    businessKey?: string;
    createdAt?: string;
    status: string;
    gesamtPreis?: number | null;

    customerId?: number | string;
}

const mapStatus = (status: string): Angebot["status"] => {
    switch (status) {
        case "IN_BEARBEITUNG":
            return "Erstellt";
        case "VERSENDET":
            return "Versendet";
        case "ANGENOMMEN":
            return "Angenommen";
        case "ABGELEHNT":
            return "Abgelehnt";
        default:
            return "Erstellt";
    }
};

export function mapOfferDTOToAngebot(dto: OfferDTO): Angebot {
    return {
        id: String(dto.id),
        angebotsnummer: dto.businessKey ?? "",
        vorname: "",
        nachname: "",
        strasse: "",
        hausnummer: "",
        plz: "",
        ort: "",
        datum: dto.createdAt?.split("T")[0] ?? "",
        erstelltAm: dto.createdAt ?? "",
        status: mapStatus(dto.status),
        betrag: dto.gesamtPreis ?? 0,
    };
}
interface RechnungDTO {
    id: string;
    rechnungsnummer: string;
    customerId: string;
    erstelldatum: string;
    faelligkeitsdatum: string;
    status: string;
    betrag: number;
}

const mapRechnungStatus = (status: string): RechnungStatus => {
    switch (status) {
        case "ERSTELLT":
            return "Erstellt";
        case "VERSENDET":
            return "Versendet";
        case "BEZAHLT":
            return "Bezahlt";
        case "IM_ZAHLUNGSVERZUG":
            return "Im Zahlungsverzug";
        default:
            return "Erstellt";
    }
};

export function mapRechnungDTOToRechnung(dto: RechnungDTO): Rechnung {
    return {
        id: dto.id,
        rechnungsnummer: dto.rechnungsnummer,

        vorname: "",
        nachname: "",
        strasse: "",
        hausnummer: "",
        plz: "",
        ort: "",

        erstelldatum: dto.erstelldatum?.split("T")[0] ?? "",
        faelligkeitsdatum: dto.faelligkeitsdatum?.split("T")[0] ?? "",
        erstelltAm: dto.erstelldatum ?? "",
        status: mapRechnungStatus(dto.status),
        betrag: dto.betrag ?? 0,
    };
}