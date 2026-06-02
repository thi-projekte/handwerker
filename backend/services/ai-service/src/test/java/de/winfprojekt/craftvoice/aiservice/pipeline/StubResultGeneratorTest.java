package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.Position;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Sichert die fachlichen Garantien des Stub-Generators ab — vor allem den harten
 * Datenschutz-Constraint (keine Preise) und die vom Vertrag erwartete Objektstruktur.
 */
class StubResultGeneratorTest {

    private final StubResultGenerator generator = new StubResultGenerator();

    @Test
    void erstangebot_liefert_vollstaendige_struktur() {
        ErgebnisKi result = generator.forErstangebot(
                new ProcessRequest("BK-1", "x", null, "schnipsel", null, null));

        assertNotNull(result.strukturierteAngebotspositionen());
        assertNotNull(result.strukturierteAngebotspositionen().leistungen());
        assertNotNull(result.strukturierteAngebotspositionen().material());
        assertNotNull(result.korrekturvorschlaege());
    }

    @Test
    void korrektur_liefert_vollstaendige_struktur() {
        ErgebnisKi result = generator.forKorrektur(
                new ProcessRequest("BK-2", "x", null, null, null, "korrektur"));

        assertNotNull(result.strukturierteAngebotspositionen());
        assertFalse(result.strukturierteAngebotspositionen().leistungen().isEmpty());
    }

    /**
     * Position hat per Design kein preis-Feld. Dieser Test prueft die Pflichtfelder
     * aller erzeugten Positionen — die Abwesenheit von preis garantiert das Record
     * selbst (siehe auch {@link #position_record_hat_genau_vier_komponenten()}).
     */
    @Test
    void positionen_tragen_pflichtfelder() {
        ErgebnisKi erst = generator.forErstangebot(
                new ProcessRequest("BK-1", "x", null, "s", null, null));
        ErgebnisKi korr = generator.forKorrektur(
                new ProcessRequest("BK-2", "x", null, null, null, "k"));

        Stream.of(erst, korr).forEach(e -> {
            var pos = e.strukturierteAngebotspositionen();
            Stream.concat(pos.leistungen().stream(), pos.material().stream())
                    .forEach(p -> {
                        assertNotNull(p.bezeichnung());
                        assertNotNull(p.einheit());
                    });
        });
    }

    @Test
    void position_record_hat_genau_vier_komponenten() {
        // Strukturwaechter: schlaegt an, sobald jemand ein Feld (z.B. preis) ergaenzt.
        assertEquals(4, Position.class.getRecordComponents().length,
                "Position muss exakt {bezeichnung, beschreibung, menge, einheit} haben — "
                        + "kein preis!");
    }
}
