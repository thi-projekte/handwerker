package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit-Tests fuer die Deserialisierung von {@link ProcessRequest}.
 *
 * <p>Stellt sicher, dass die typisierten Records aus {@link ProcessRequest},
 * {@link Vorlage}, {@link Kundendaten}, {@link Angebotsentwurf} und
 * {@link AngebotsPosition} sowohl die Erstangebot- als auch die Korrektur-Payload
 * der Process Engine korrekt parsen.
 */
class ProcessRequestParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parses_erstangebot_payload() throws Exception {
        String json = """
                {
                  "businessKey": "BK-001",
                  "processInstanceId": "PI-aaaa-1111",
                  "kundendaten": {
                    "name": "Mueller GmbH",
                    "adresse": "Hauptstrasse 12, 80331 Muenchen"
                  },
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
        assertEquals("PI-aaaa-1111", req.processInstanceId());

        assertNotNull(req.kundendaten());
        assertEquals("Mueller GmbH", req.kundendaten().name());

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
                  "processInstanceId": "PI-bbbb-2222",
                  "kundendaten": {
                    "name": "Schmidt"
                  },
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
        assertNotNull(req.kundendaten());
        assertEquals("Schmidt", req.kundendaten().name());

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
        // Zusaetzliche Top-Level-Felder, die wir nicht modelliert haben,
        // duerfen den Parser NICHT zum Absturz bringen.
        String json = """
                {
                  "businessKey": "BK-003",
                  "debugInfo": "egal",
                  "irgendwasNeues": { "foo": "bar" }
                }
                """;

        ProcessRequest req = mapper.readValue(json, ProcessRequest.class);

        assertEquals("BK-003", req.businessKey());
    }
}
