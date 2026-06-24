import { Angebot } from "@/features/document/types/document.types";

interface OfferDTO {
    id: number | string;
    businessKey?: string;
    createdAt?: string;
    status: string;
    gesamtPreis?: number;

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