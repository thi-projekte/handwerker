package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit-Tests fuer die Deserialisierung von {@link ProcessRequest}.
 *
 * <p>Die Beispiel-JSONs spiegeln die echte Connector-Payload-Form
 * aus {@code Sprachschnipselverarbeitung.bpmn} wider (siehe
 * {@code docs/bpmn-reference/}). KEINE {@code kundendaten}, KEIN
 * {@code processInstanceId} — nur was die BPMN-Engine wirklich schickt.
 */
class ProcessRequestParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parses_erstangebot_payload() throws Exception {
        String json = """
                {
                  "businessKey": "BK-001",
                  "prompt": "Erstelle ein Erstangebot anhand von Vorlage und Sprachschnipsel.",
                  "vorlage": {
                    "leistungen": ["Fliesenlegen 45 EUR/h", "Verfugen 35 EUR/h"],
                    "material": ["Feinsteinzeug 60x60 matt"],
                    "notizen": ["Kunde bevorzugt grosse Formate"]
                  },
                  "sprachschnipsel": "Im Bad neue Bodenfliesen verlegen, ca. 15 Quadratmeter, grossformatig."
                }
                """;

        ProcessRequest req = mapper.readValue(json, ProcessRequest.class);

        assertEquals("BK-001", req.businessKey());
        assertNotNull(req.prompt());
        assertEquals("Erstelle ein Erstangebot anhand von Vorlage und Sprachschnipsel.",
                req.prompt());

        assertNotNull(req.vorlage());
        assertEquals(2, req.vorlage().leistungen().size());
        assertEquals("Feinsteinzeug 60x60 matt", req.vorlage().material().get(0));

        assertNotNull(req.sprachschnipsel());

        // Erstangebot-Fall: angebotsentwurf + korrekturschnipsel sind null
        assertNull(req.angebotsentwurf());
        assertNull(req.korrekturschnipsel());
    }

    @Test
    void parses_korrektur_payload() throws Exception {
        String json = """
                {
                  "businessKey": "BK-002",
                  "prompt": "Ueberarbeite den Angebotsentwurf anhand des Korrekturschnipsels.",
                  "angebotsentwurf": {
                    "strukturierteAngebotspositionen": [
                      {
                        "bezeichnung": "Bodenfliesen Feinsteinzeug 60x60",
                        "beschreibung": "Verlegung im Badezimmer",
                        "menge": 15.0,
                        "einheit": "m2"
                      }
                    ]
                  },
                  "korrekturschnipsel": "Bitte zusaetzlich noch Sockelleisten einplanen."
                }
                """;

        ProcessRequest req = mapper.readValue(json, ProcessRequest.class);

        assertEquals("BK-002", req.businessKey());
        assertEquals("Ueberarbeite den Angebotsentwurf anhand des Korrekturschnipsels.",
                req.prompt());

        assertNotNull(req.angebotsentwurf());
        assertEquals(1, req.angebotsentwurf().strukturierteAngebotspositionen().size());

        AngebotsPosition pos = req.angebotsentwurf().strukturierteAngebotspositionen().get(0);
        assertEquals("Bodenfliesen Feinsteinzeug 60x60", pos.bezeichnung());
        assertEquals(15.0, pos.menge());
        assertEquals("m2", pos.einheit());

        assertNotNull(req.korrekturschnipsel());

        // Korrektur-Fall: vorlage + sprachschnipsel sind null
        assertNull(req.vorlage());
        assertNull(req.sprachschnipsel());
    }

    @Test
    void parses_payload_with_unknown_fields() throws Exception {
        // Zusaetzliche Top-Level-Felder, die wir nicht modelliert haben (z.B. kundendaten,
        // processInstanceId aus alten Versionen oder Debug-Infos), duerfen den Parser
        // NICHT zum Absturz bringen.
        String json = """
                {
                  "businessKey": "BK-003",
                  "kundendaten": { "name": "Mueller" },
                  "processInstanceId": "PI-veraltet",
                  "debugInfo": "egal"
                }
                """;

        ProcessRequest req = mapper.readValue(json, ProcessRequest.class);

        assertEquals("BK-003", req.businessKey());
    }
}
