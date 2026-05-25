package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.model.AngebotsPosition;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fuer den Stub-Generator. Stellt sicher, dass die Mock-Antwort:
 * <ul>
 *   <li>mindestens 2 Positionen enthaelt (Akzeptanzkriterium #532)</li>
 *   <li>nichtleere Korrekturvorschlaege enthaelt</li>
 *   <li>NIE ein Preis-Feld enthaelt, auch nicht nach JSON-Serialisierung
 *       (harter Datenschutz-Constraint)</li>
 *   <li>fuer Erstangebot UND Korrektur unterschiedliche Daten zurueckgibt
 *       (damit der manuelle Test den Unterschied sieht)</li>
 * </ul>
 */
class StubResultGeneratorTest {

    private final StubResultGenerator generator = new StubResultGenerator();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void erstangebot_liefert_mindestens_zwei_positionen() {
        ErgebnisKi result = generator.forErstangebot(dummyRequest());

        assertNotNull(result);
        assertTrue(result.strukturierteAngebotspositionen().size() >= 2,
                "Stub soll mindestens 2 Positionen liefern");
    }

    @Test
    void erstangebot_liefert_nichtleere_korrekturvorschlaege() {
        ErgebnisKi result = generator.forErstangebot(dummyRequest());

        assertNotNull(result.korrekturvorschlaege());
        assertFalse(result.korrekturvorschlaege().isEmpty(),
                "Stub soll mindestens einen Korrekturhinweis liefern");
    }

    @Test
    void korrektur_liefert_eigene_realistische_daten() {
        ErgebnisKi erstangebot = generator.forErstangebot(dummyRequest());
        ErgebnisKi korrektur = generator.forKorrektur(dummyRequest());

        assertNotNull(korrektur);
        assertFalse(korrektur.strukturierteAngebotspositionen().isEmpty());
        assertFalse(korrektur.korrekturvorschlaege().isEmpty());

        // Erstangebot und Korrektur sollen unterschiedlich sein - sonst sieht man im
        // manuellen Test nicht, ob das Routing wirklich greift.
        assertFalse(
                erstangebot.strukturierteAngebotspositionen().size()
                        == korrektur.strukturierteAngebotspositionen().size()
                && erstangebot.korrekturvorschlaege().equals(korrektur.korrekturvorschlaege()),
                "Erstangebot- und Korrektur-Stub sollten unterscheidbar sein"
        );
    }

    @Test
    void positionen_enthalten_alle_pflichtfelder() {
        ErgebnisKi result = generator.forErstangebot(dummyRequest());

        for (AngebotsPosition p : result.strukturierteAngebotspositionen()) {
            assertNotNull(p.bezeichnung(), "bezeichnung darf nicht null sein");
            assertNotNull(p.beschreibung(), "beschreibung darf nicht null sein");
            assertNotNull(p.menge(), "menge darf nicht null sein");
            assertNotNull(p.einheit(), "einheit darf nicht null sein");
        }
    }

    @Test
    void serialisiertes_json_enthaelt_KEIN_preis_feld() throws Exception {
        // Defensiver Test: Selbst wenn jemand das Record erweitert, fliegt
        // sofort auf wenn 'preis' im JSON auftaucht. Datenschutz-Constraint.
        ErgebnisKi result = generator.forErstangebot(dummyRequest());
        String json = mapper.writeValueAsString(result);

        assertFalse(json.toLowerCase().contains("preis"),
                "Stub-JSON darf KEIN 'preis'-Feld enthalten (Datenschutz): " + json);

        // Doppelt sicher via JsonNode-Traversal
        JsonNode tree = mapper.readTree(json);
        for (JsonNode pos : tree.get("strukturierteAngebotspositionen")) {
            assertFalse(pos.has("preis"),
                    "Keine Position darf ein 'preis'-Feld haben");
        }
    }

    private ProcessRequest dummyRequest() {
        return new ProcessRequest("BK-TEST", null, null, null, null, null);
    }
}
