import { useEffect, useState } from "react";
import { getDocuments } from "@/data/api/documentApi";
import { mapOfferDTOToAngebot } from "../mapper/documentMapper";
import { Angebot } from "../types/document.types";
import { getCustomer } from "@/data/api/customerApi";
import { mapCustomerDTO } from "@/features/document/mapper/customerMapper";

export const useDocuments = () => {
  const [data, setData] = useState<Angebot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);

        const offers = await getDocuments();

        const mapped = await Promise.all(
          offers.map(async (dto) => {
            const angebot = mapOfferDTOToAngebot(dto);

            if (!dto.customerId) return angebot;

            const customerDTO = await getCustomer(dto.customerId);
            const customer = mapCustomerDTO(customerDTO);

            return {
              ...angebot,
              vorname: customer.vorname,
              nachname: customer.nachname,
              strasse: customer.strasse,
              hausnummer: customer.hausnummer,
              plz: customer.plz,
              ort: customer.ort,
            };
          })
        );

        setData(mapped);
      } catch (e) {
        setError(e instanceof Error ? e : new Error("Unknown error"));
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  return { data, loading, error };
};