import type { Angebot, Rechnung, RechnungStatus } from "@/features/document/types/document.types";

interface OfferDTO {
    id: number | string;
    businessKey?: string;
    createdAt?: string;
    status: string;
    gesamtPreis?: number | null;

    customer?: {
        vorname?: string;
        nachname?: string;
        strasse?: string;
        hausnummer?: string;
        plz?: string;
        ort?: string;
    };
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
        vorname: dto.customer?.vorname ?? "",
        nachname: dto.customer?.nachname ?? "",
        strasse: dto.customer?.strasse ?? "",
        hausnummer: dto.customer?.hausnummer ?? "",
        plz: dto.customer?.plz ?? "",
        ort: dto.customer?.ort ?? "",
        datum: dto.createdAt?.split("T")[0] ?? "",
        status: mapStatus(dto.status),
        betrag: dto.gesamtPreis ?? 0,
    };
}
interface RechnungDTO {
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
        vorname: dto.vorname,
        nachname: dto.nachname,
        strasse: dto.strasse,
        hausnummer: dto.hausnummer,
        plz: dto.plz,
        ort: dto.ort,
        erstelldatum: dto.erstelldatum?.split("T")[0] ?? "",
        faelligkeitsdatum: dto.faelligkeitsdatum?.split("T")[0] ?? "",
        status: mapRechnungStatus(dto.status),
        betrag: dto.betrag ?? 0,
    };
}