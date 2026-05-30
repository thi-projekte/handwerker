package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stellt sicher, dass das vom Camunda HTTP-Connector geschickte JSON exakt auf unsere
 * {@link ProcessRequest}-Records gemappt wird — inklusive der Faelle, in denen Felder
 * fehlen (Jackson soll dann {@code null} setzen, nicht crashen).
 *
 * <p>Diese Tests sind die Gegenprobe zu den BPMN-Payloads (Schnittstellenvertrag
 * Stand 29.05.2026): aendert das BPMN-Team die Feldnamen oder die Struktur, brechen
 * hier die Assertions.
 */
class ProcessRequestParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void erstangebot_payload_wird_vollstaendig_gemappt() throws Exception {
        String json = """
                {
                  "businessKey": "BK-100",
                  "prompt": "Erstelle ein Erstangebot anhand von Vorlage und Sprachschnipsel.",
                  "vorlage": {
                    "leistungen": [
                      {"bezeichnung": "Fliesen verlegen", "beschreibung": "Stundensatz", "menge": 1, "einheit": "h"}
                    ],
                    "material": [
                      {"bezeichnung": "Feinsteinzeug 60x60", "beschreibung": "grossformatig", "menge": 15, "einheit": "m²"}
                    ],
                    "notizen": ["grossformatig"]
                  },
                  "sprachschnipsel": "Im Bad neue Bodenfliesen verlegen."
                }
                """;

        ProcessRequest req = mapper.readValue(json, ProcessRequest.class);

        assertEquals("BK-100", req.businessKey());
        assertEquals("Erstelle ein Erstangebot anhand von Vorlage und Sprachschnipsel.", req.prompt());
        assertNotNull(req.vorlage());
        assertEquals(1, req.vorlage().leistungen().size());
        assertEquals("Fliesen verlegen", req.vorlage().leistungen().get(0).bezeichnung());
        assertEquals("Feinsteinzeug 60x60", req.vorlage().material().get(0).bezeichnung());
        assertEquals(15.0, req.vorlage().material().get(0).menge());
        assertEquals("Im Bad neue Bodenfliesen verlegen.", req.sprachschnipsel());
        assertNull(req.strukturierteAngebotspositionen());
        assertNull(req.korrekturschnipsel());
    }

    @Test
    void korrektur_payload_wird_vollstaendig_gemappt() throws Exception {
        String json = """
                {
                  "businessKey": "BK-200",
                  "prompt": "Überarbeite die strukturierten Angebotspositionen anhand des Korrekturschnipsels.",
                  "strukturierteAngebotspositionen": {
                    "leistungen": [
                      {"bezeichnung": "Bodenfliesen", "beschreibung": "Feinsteinzeug", "menge": 15.0, "einheit": "m2"}
                    ],
                    "material": [],
                    "notizen": ["bestehender Stand"]
                  },
                  "korrekturschnipsel": "Bitte zusaetzlich Sockelleisten einplanen."
                }
                """;

        ProcessRequest req = mapper.readValue(json, ProcessRequest.class);

        assertEquals("BK-200", req.businessKey());
        assertNull(req.vorlage());
        assertNull(req.sprachschnipsel());
        assertNotNull(req.strukturierteAngebotspositionen());
        assertEquals(1, req.strukturierteAngebotspositionen().leistungen().size());
        assertEquals("Bodenfliesen",
                req.strukturierteAngebotspositionen().leistungen().get(0).bezeichnung());
        assertEquals("Bitte zusaetzlich Sockelleisten einplanen.", req.korrekturschnipsel());
    }

    @Test
    void leeres_json_wird_zu_lauter_null_feldern() throws Exception {
        ProcessRequest req = mapper.readValue("{}", ProcessRequest.class);

        assertNull(req.businessKey());
        assertNull(req.vorlage());
        assertNull(req.sprachschnipsel());
        assertNull(req.strukturierteAngebotspositionen());
        assertNull(req.korrekturschnipsel());
    }

    @Test
    void unbekannte_felder_brechen_nicht() throws Exception {
        // Robustheit gegen BPMN-Zusatzfelder (z.B. customerId, das wir bewusst ignorieren)
        String json = """
                {
                  "businessKey": "BK-300",
                  "vorlage": {"leistungen": [], "material": [], "notizen": []},
                  "sprachschnipsel": "test",
                  "rawSnippet": "etwas, das wir (noch) nicht kennen",
                  "customerId": 12345
                }
                """;

        ProcessRequest req = mapper.readValue(json, ProcessRequest.class);

        assertEquals("BK-300", req.businessKey());
        assertEquals("test", req.sprachschnipsel());
    }
}
