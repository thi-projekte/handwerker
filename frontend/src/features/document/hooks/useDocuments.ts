import { useEffect, useState } from "react";
import { getDocuments } from "@/data/api/documentApi";
import { mapOfferDTOToAngebot } from "../mapper/documentMapper";
import { Angebot } from "../types/document.types";

export const useDocuments = () => {
  const [data, setData] = useState<Angebot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);

        const res = await getDocuments();

        setData(res.map(mapOfferDTOToAngebot));
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