package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.CatalogCandidate;
import de.winfprojekt.craftvoice.aiservice.client.MegaLlmClient;
import de.winfprojekt.craftvoice.aiservice.model.Position;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests fuer die Auswahl-Logik des {@link Call2Selector} im Baseline-Modus
 * (kein API-Key → Top-Kandidat). Der parallele {@code enrich(...)} wird hier nicht getestet
 * (ueber {@code selectFor}, ohne Executor); das Zusammenspiel deckt der QuarkusTest ab.
 */
class Call2SelectorTest {

    private CatalogSearchService catalogSearch;
    private Call2Selector selector;

    @BeforeEach
    void setUp() {
        catalogSearch = Mockito.mock(CatalogSearchService.class);
        // Kein API-Key -> Baseline-Pfad
        MegaLlmService megaLlm = new MegaLlmService(Mockito.mock(MegaLlmClient.class), Optional.empty());
        selector = new Call2Selector(
                megaLlm, catalogSearch, new Call2PromptBuilder(new ObjectMapper()),
                null, "gemini-3-flash-preview");
    }

    @Test
    void ohneKey_nimmtTopKandidatAlsBaseline() {
        when(catalogSearch.search(anyString(), anyInt(), any())).thenReturn(List.of(
                new CatalogCandidate("1001", "FLI-1001", "Feinsteinzeug Eiche", "desc", "m2", "Fliesen", 3.0),
                new CatalogCandidate("1002", "FLI-1002", "Feinsteinzeug Beton", "desc", "m2", "Fliesen", 2.0)));

        Position result = selector.selectFor(
                new Position("Feinsteinzeug 60x60", "Bodenfliese", 15.0, "m2"), "owner-1");

        assertEquals("1001", result.katalogProduktId());
    }

    @Test
    void keineKandidaten_lassenPositionUnveraendert() {
        when(catalogSearch.search(anyString(), anyInt(), any())).thenReturn(List.of());

        Position input = new Position("Exotenartikel", "gibt es nicht", 1.0, "Stk");
        Position result = selector.selectFor(input, "owner-1");

        assertNull(result.katalogProduktId());
        assertEquals("Exotenartikel", result.bezeichnung());
    }
}
