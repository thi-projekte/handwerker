import type { CustomerDTO } from "@/data/api/customerApi";

export interface Customer {
  id: string;
  vorname: string;
  nachname: string;
  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;
}

export function mapCustomerDTO(dto: CustomerDTO): Customer {
  return {
    id: String(dto.id),
    vorname: dto.firstName ?? "",
    nachname: dto.lastName ?? "",
    strasse: dto.street ?? "",
    hausnummer: dto.houseNumber ?? "",
    plz: dto.zipCode ?? "",
    ort: dto.city ?? "",
  };
}