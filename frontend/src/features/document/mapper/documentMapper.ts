import type { Angebot, Rechnung } from "@/features/document/types/document.types";

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
    id: number | string;
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

export function mapRechnungDTOToRechnung(dto: RechnungDTO): Rechnung {
    return {
        id: String(dto.id),
        rechnungsnummer: dto.rechnungsnummer,
        offerBusinessKey: dto.offerBusinessKey,

        vorname: dto.kundendaten.vorname,
        nachname: dto.kundendaten.nachname,

        strasse: dto.kundendaten.strasse,
        hausnummer: dto.kundendaten.hausnummer,
        plz: dto.kundendaten.plz,
        ort: dto.kundendaten.ort,

        erstelldatum: dto.createdAt.split("T")[0],

        // Das Backend liefert aktuell kein Fälligkeitsdatum
        faelligkeitsdatum: "",

        erstelltAm: dto.createdAt,

        // Das Backend liefert aktuell keinen Status
        status: "Erstellt",

        betrag: dto.gesamtPreis,
    };
}