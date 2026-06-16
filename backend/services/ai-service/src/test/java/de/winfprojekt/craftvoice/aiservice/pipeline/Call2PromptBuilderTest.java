package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.CatalogCandidate;
import de.winfprojekt.craftvoice.aiservice.model.Position;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Tests fuer den Call-2-Prompt-Aufbau und das Parsen der Modell-Antwort.
 */
class Call2PromptBuilderTest {

    private final Call2PromptBuilder builder = new Call2PromptBuilder(new ObjectMapper());

    private static CatalogCandidate cand(String art, String name) {
        return new CatalogCandidate("1", art, name, "Beschreibung " + name, "Stk", "Kategorie", null);
    }

    @Test
    void systemPrompt_erlaubtKeinTreffer_undIgnoriertPreise() {
        String s = builder.system();
        assertTrue(s.contains("KEIN_TREFFER"));
        assertTrue(s.toLowerCase().contains("preise"));
    }

    @Test
    void userContent_listetKandidaten_ohnePreis_neutralSortiert() {
        Position pos = new Position("Doppelsteckdose", "UP 2-fach", 3.0, "Stk");
        String u = builder.userContent(pos, List.of(
                cand("ELE-3002", "Wechselschalter"),
                cand("ELE-3001", "Doppelsteckdose UP")));

        assertTrue(u.contains("Doppelsteckdose"));
        assertTrue(u.contains("ELE-3001"));
        assertTrue(u.contains("ELE-3002"));
        // neutral nach articleNumber sortiert -> ELE-3001 erscheint vor ELE-3002
        assertTrue(u.indexOf("ELE-3001") < u.indexOf("ELE-3002"));
        // keine Preis-Information im Prompt
        assertFalse(u.toLowerCase().contains("preis"));
        assertFalse(u.contains("€"));
    }

    @Test
    void parsePick_ausSauberemJson() {
        assertEquals("FLI-1001",
                builder.parsePick("{\"articleNumber\":\"FLI-1001\",\"begruendung\":\"passt\"}"));
    }

    @Test
    void parsePick_mitMarkdownFences() {
        assertEquals("ELE-3001",
                builder.parsePick("```json\n{\"articleNumber\":\"ELE-3001\"}\n```"));
    }

    @Test
    void parsePick_keinTreffer_ausJson() {
        assertEquals(Call2PromptBuilder.KEIN_TREFFER,
                builder.parsePick("{\"articleNumber\":\"KEIN_TREFFER\"}"));
    }

    @Test
    void parsePick_keinTreffer_ausFreitext() {
        assertEquals(Call2PromptBuilder.KEIN_TREFFER, builder.parsePick("Kein Treffer leider"));
    }

    @Test
    void parsePick_regexFallback_ausFreitext() {
        assertEquals("BOD-2001", builder.parsePick("Ich waehle BOD-2001 weil es am besten passt."));
    }

    @Test
    void parsePick_muell_wirdZuKeinTreffer() {
        assertEquals(Call2PromptBuilder.KEIN_TREFFER, builder.parsePick("völliger Unsinn ohne Nummer"));
    }
}
