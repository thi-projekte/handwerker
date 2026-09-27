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
        assertFalse(result.strukturierteAngebotspositionen().material().isEmpty());
        assertNotNull(result.korrekturvorschlaege());
        // Demo-Vorgabe: der Stub setzt immer 4 Stunden Arbeitszeit.
        assertEquals(4.0, result.geschaetzteArbeitsdauerStunden(), 0.0);
    }

    @Test
    void korrektur_liefert_vollstaendige_struktur() {
        ErgebnisKi result = generator.forKorrektur(
                new ProcessRequest("BK-2", "x", null, null, null, "korrektur"));

        assertNotNull(result.strukturierteAngebotspositionen());
        // Inhalt liegt in den Materialpositionen (Arbeitszeit separat via Stundenfeld).
        assertFalse(result.strukturierteAngebotspositionen().material().isEmpty());
        assertEquals(4.0, result.geschaetzteArbeitsdauerStunden(), 0.0);
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
    void position_record_hat_kein_preis_feld() {
        // Strukturwaechter (Datenschutz): Position darf NIE ein Preis-Feld bekommen.
        // katalogProduktId (Call 2, #541) ist KEIN Preis, sondern eine Katalog-Referenz.
        var namen = Stream.of(Position.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
        assertFalse(
                namen.stream().anyMatch(n -> n.toLowerCase().contains("preis")
                        || n.toLowerCase().contains("price")),
                "Position darf kein Preis-Feld haben (Datenschutz-Constraint).");
        // Erwartete Struktur inkl. katalogProduktId.
        assertEquals(
                java.util.List.of("bezeichnung", "beschreibung", "menge", "einheit", "katalogProduktId"),
                namen,
                "Unerwartete Position-Struktur — Vertrag pruefen.");
    }
}
