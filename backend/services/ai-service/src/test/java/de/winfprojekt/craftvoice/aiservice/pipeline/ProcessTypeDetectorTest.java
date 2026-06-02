package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.Angebotspositionen;
import de.winfprojekt.craftvoice.aiservice.model.Position;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;
import de.winfprojekt.craftvoice.aiservice.model.Vorlage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueft die Routing-Logik des {@link ProcessTypeDetector} — die Fallunterscheidung
 * Erstangebot vs. Korrektur erfolgt allein ueber Feld-Anwesenheit, daher ist sie der
 * fehleranfaelligste Punkt der Schnittstelle und gehoert dicht getestet.
 */
class ProcessTypeDetectorTest {

    private final ProcessTypeDetector detector = new ProcessTypeDetector();

    private static Vorlage vorlage() {
        return new Vorlage(
                List.of(new Position("Fliesen verlegen", "Bad", 15.0, "m²")),
                List.of(new Position("Feinsteinzeug", "60x60", 15.0, "m²")),
                List.of("Vor Ort prüfen"));
    }

    private static Angebotspositionen positionen() {
        return new Angebotspositionen(
                List.of(new Position("Fliesen verlegen", "Bad", 15.0, "m²")),
                List.of(new Position("Feinsteinzeug", "60x60", 15.0, "m²")),
                List.of());
    }

    @Test
    void erkennt_erstangebot_wenn_vorlage_und_sprachschnipsel_gesetzt() {
        ProcessRequest req = new ProcessRequest(
                "BK-1", "Erstelle Angebot", vorlage(), "Bad neu fliesen", null, null);

        assertEquals(ProcessType.ERSTANGEBOT, detector.determine(req));
    }

    @Test
    void erkennt_korrektur_wenn_positionen_und_korrekturschnipsel_gesetzt() {
        ProcessRequest req = new ProcessRequest(
                "BK-2", "Überarbeite", null, null, positionen(), "Sockelleisten ergänzen");

        assertEquals(ProcessType.KORREKTUR, detector.determine(req));
    }

    @Test
    void korrektur_hat_vorrang_wenn_beide_feldgruppen_vorhanden() {
        // Spiegelt das BPMN-Script: Korrektur wird vor Erstangebot geprueft.
        ProcessRequest req = new ProcessRequest(
                "BK-3", "x", vorlage(), "schnipsel", positionen(), "korrektur");

        assertEquals(ProcessType.KORREKTUR, detector.determine(req));
    }

    @Test
    void wirft_wenn_request_null() {
        assertThrows(IllegalArgumentException.class, () -> detector.determine(null));
    }

    @Test
    void wirft_wenn_keine_feldgruppe_vollstaendig() {
        // nur businessKey + prompt, keine inhaltlichen Felder
        ProcessRequest req = new ProcessRequest("BK-4", "x", null, null, null, null);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> detector.determine(req));
        assertTrue(ex.getMessage().contains("BK-4"),
                "Fehlermeldung sollte den businessKey zur Diagnose enthalten");
    }

    @Test
    void wirft_wenn_erstangebot_unvollstaendig() {
        // vorlage ohne sprachschnipsel ist kein gueltiges Erstangebot
        ProcessRequest req = new ProcessRequest("BK-5", "x", vorlage(), null, null, null);

        assertThrows(IllegalArgumentException.class, () -> detector.determine(req));
    }

    @Test
    void wirft_wenn_korrektur_unvollstaendig() {
        // korrekturschnipsel ohne strukturierteAngebotspositionen ist keine gueltige Korrektur
        ProcessRequest req = new ProcessRequest("BK-6", "x", null, null, null, "korrektur");

        assertThrows(IllegalArgumentException.class, () -> detector.determine(req));
    }
}
