import { useEffect, useState } from "react";
import {
    getAngebote,
    getRechnungen,
    AngebotDTO,
    RechnungDTO,
} from "@/data/api/documentApi";

interface UseDocumentsState {
    angebote: AngebotDTO[];
    rechnungen: RechnungDTO[];
    loading: boolean;
    error: Error | null;
}

export const useDocuments = () => {
    const [state, setState] = useState<UseDocumentsState>({
        angebote: [],
        rechnungen: [],
        loading: true,
        error: null,
    });

    useEffect(() => {
        const loadDocuments = async () => {
            try {
                setState((prev) => ({
                    ...prev,
                    loading: true,
                    error: null,
                }));

                const [angebote, rechnungen] = await Promise.all([
                    getAngebote(),
                    getRechnungen(),
                ]);

                setState({
                    angebote,
                    rechnungen,
                    loading: false,
                    error: null,
                });
            } catch (error) {
                setState({
                    angebote: [],
                    rechnungen: [],
                    loading: false,
                    error:
                        error instanceof Error
                            ? error
                            : new Error("Unbekannter Fehler"),
                });
            }
        };

        loadDocuments();
    }, []);

    return state;
};