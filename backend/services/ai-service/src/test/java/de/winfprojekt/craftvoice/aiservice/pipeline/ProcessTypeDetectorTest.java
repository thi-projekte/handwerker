package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.Angebotsentwurf;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;
import de.winfprojekt.craftvoice.aiservice.model.Vorlage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Tests fuer die Fallunterscheidung in {@link ProcessTypeDetector}.
 *
 * <p>Wir instanziieren den Detector direkt ohne CDI-Container — die Klasse hat keinen
 * State und keine Abhaengigkeiten, also ist {@code new ...()} hier voellig okay
 * und ein {@code @QuarkusTest} waere Overkill.
 *
 * <p>Reihenfolge der {@link ProcessRequest}-Felder:
 * businessKey, prompt, vorlage, sprachschnipsel, angebotsentwurf, korrekturschnipsel.
 */
class ProcessTypeDetectorTest {

    private final ProcessTypeDetector detector = new ProcessTypeDetector();

    private static Vorlage leereVorlage() {
        return new Vorlage(List.of(), List.of(), List.of());
    }

    private static Angebotsentwurf leererEntwurf() {
        return new Angebotsentwurf(List.of());
    }

    @Test
    void liefert_erstangebot_bei_vorlage_und_sprachschnipsel() {
        ProcessRequest req = new ProcessRequest(
                "BK-1", "Erstangebot-Prompt", leereVorlage(), "Bad sanieren", null, null
        );

        assertEquals(ProcessType.ERSTANGEBOT, detector.determine(req));
    }

    @Test
    void liefert_korrektur_bei_angebotsentwurf_und_korrekturschnipsel() {
        ProcessRequest req = new ProcessRequest(
                "BK-2", "Korrektur-Prompt", null, null, leererEntwurf(), "Bitte Sockel ergaenzen"
        );

        assertEquals(ProcessType.KORREKTUR, detector.determine(req));
    }

    @Test
    void wirft_exception_wenn_keine_felder_gesetzt() {
        ProcessRequest req = new ProcessRequest(
                "BK-3", null, null, null, null, null
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> detector.determine(req)
        );
        assertTrue(ex.getMessage().contains("BK-3"),
                "Fehlermeldung soll den businessKey enthalten");
    }

    @Test
    void wirft_exception_wenn_nur_sprachschnipsel_ohne_vorlage() {
        ProcessRequest req = new ProcessRequest(
                "BK-4", null, null, "Schnipsel ohne Vorlage", null, null
        );

        assertThrows(IllegalArgumentException.class, () -> detector.determine(req));
    }

    @Test
    void wirft_exception_wenn_nur_vorlage_ohne_sprachschnipsel() {
        ProcessRequest req = new ProcessRequest(
                "BK-5", null, leereVorlage(), null, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> detector.determine(req));
    }

    @Test
    void wirft_exception_bei_null_request() {
        assertThrows(IllegalArgumentException.class, () -> detector.determine(null));
    }

    @Test
    void bevorzugt_korrektur_wenn_beide_faelle_gleichzeitig_gesetzt() {
        // Edge-Case: PE schickt fehlerhaft alles. Unser aktuelles Verhalten:
        // Korrektur gewinnt. Dokumentiert hier, falls jemand das Verhalten aendert.
        ProcessRequest req = new ProcessRequest(
                "BK-6", "Beide-Prompt",
                leereVorlage(), "Spachnipsel-Text",
                leererEntwurf(), "Korrektur-Text"
        );

        assertEquals(ProcessType.KORREKTUR, detector.determine(req));
    }
}
