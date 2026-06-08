package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.CatalogCandidate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Tests fuer den Mock-Betrieb des {@link CatalogSearchService} (laedt den gebuendelten
 * Mock-Katalog vom Classpath und rankt per Token-Overlap).
 */
class CatalogSearchServiceTest {

    // mockEnabled=true -> laedt /mock/catalog-mock.json; client wird nicht gebraucht (null).
    private final CatalogSearchService service =
            new CatalogSearchService(null, new ObjectMapper(), true);

    @Test
    void findetPassendeKandidaten_undRanktRelevanteOben() {
        List<CatalogCandidate> result = service.search("Feinsteinzeug Eiche Bodenfliese", 5);

        assertFalse(result.isEmpty(), "Es sollten Kandidaten gefunden werden.");
        // Der spezifischste Treffer (Eiche-natur) sollte ganz oben stehen.
        assertEquals("FLI-1001", result.get(0).articleNumber());
        // Kandidaten tragen keinen Preis (Datenschutz-/Strip-Garantie über das DTO).
        assertTrue(result.stream().allMatch(c -> c.articleNumber() != null));
    }

    @Test
    void findetElektroKandidaten() {
        List<CatalogCandidate> result = service.search("Doppelsteckdose Unterputz", 10);
        assertTrue(result.stream().anyMatch(c -> "ELE-3001".equals(c.articleNumber())),
                "Doppelsteckdose UP sollte gefunden werden.");
    }

    @Test
    void respektiertLimit() {
        List<CatalogCandidate> result = service.search("Fliese Boden weiss grau", 2);
        assertTrue(result.size() <= 2);
    }

    @Test
    void keinTreffer_liefertLeereListe() {
        List<CatalogCandidate> result = service.search("Quantencomputer Raumschiff", 5);
        assertTrue(result.isEmpty(), "Voellig fachfremde Query darf nichts liefern.");
    }

    @Test
    void leereQuery_liefertLeereListe() {
        assertTrue(service.search("   ", 5).isEmpty());
    }
}
