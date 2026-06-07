package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.MegaLlmException;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Tests fuer das robuste Parsen der Modell-Ausgabe ({@link ErgebnisKiParser}).
 * Reine JUnit-Tests ohne Quarkus/API-Key — decken die in der Eval beobachteten
 * Toleranz-Faelle ab (Fences, fehlender Wrapper, null-Menge, kaputtes JSON).
 */
class ErgebnisKiParserTest {

    private final ErgebnisKiParser parser = new ErgebnisKiParser(new ObjectMapper());

    @Test
    void parstSauberesJson() {
        String json = """
                {"strukturierteAngebotspositionen":{
                  "leistungen":[{"bezeichnung":"Fliesen verlegen","beschreibung":"Bad","menge":15,"einheit":"m²"}],
                  "material":[],
                  "notizen":["Vor Ort pruefen"]
                },"korrekturvorschlaege":["Fugenfarbe abstimmen"]}
                """;

        ErgebnisKi result = parser.parse(json);

        assertEquals(1, result.strukturierteAngebotspositionen().leistungen().size());
        assertEquals("Fliesen verlegen",
                result.strukturierteAngebotspositionen().leistungen().get(0).bezeichnung());
        assertEquals(15.0,
                result.strukturierteAngebotspositionen().leistungen().get(0).menge());
        assertEquals(1, result.strukturierteAngebotspositionen().notizen().size());
        assertEquals(1, result.korrekturvorschlaege().size());
    }

    @Test
    void entferntMarkdownFences() {
        String json = """
                ```json
                {"strukturierteAngebotspositionen":{"leistungen":[],"material":[],"notizen":[]},"korrekturvorschlaege":[]}
                ```
                """;

        ErgebnisKi result = parser.parse(json);

        assertTrue(result.strukturierteAngebotspositionen().leistungen().isEmpty());
        assertTrue(result.korrekturvorschlaege().isEmpty());
    }

    @Test
    void normalisiertFehlendenWrapper() {
        // Manche Modelle geben den inneren Block ohne strukturierteAngebotspositionen zurueck.
        String json = """
                {"leistungen":[{"bezeichnung":"Steckdose","beschreibung":"UP","menge":3,"einheit":"Stk"}],
                 "material":[],"notizen":[]}
                """;

        ErgebnisKi result = parser.parse(json);

        assertEquals(1, result.strukturierteAngebotspositionen().leistungen().size());
        assertEquals(3.0, result.strukturierteAngebotspositionen().leistungen().get(0).menge());
        assertTrue(result.korrekturvorschlaege().isEmpty());
    }

    @Test
    void uebernimmtNullMenge() {
        String json = """
                {"strukturierteAngebotspositionen":{
                  "leistungen":[{"bezeichnung":"X","beschreibung":"Y","menge":null,"einheit":"Stk"}],
                  "material":[],"notizen":[]},"korrekturvorschlaege":[]}
                """;

        ErgebnisKi result = parser.parse(json);

        assertNull(result.strukturierteAngebotspositionen().leistungen().get(0).menge());
    }

    @Test
    void ungueltigesJson_wirftMegaLlmException() {
        assertThrows(MegaLlmException.class, () -> parser.parse("das ist kein JSON"));
    }

    @Test
    void jsonArrayStattObjekt_wirftMegaLlmException() {
        assertThrows(MegaLlmException.class, () -> parser.parse("[1,2,3]"));
    }
}
