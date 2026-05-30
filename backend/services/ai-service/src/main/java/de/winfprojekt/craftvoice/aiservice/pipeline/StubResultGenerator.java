package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.Angebotspositionen;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.Position;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Erzeugt eine fest verdrahtete {@link ErgebnisKi}-Antwort fuer die Stub-Phase
 * (Ticket #532).
 *
 * <p>So lange die echte LLM-Pipeline (Tickets #538 / #541) nicht steht, antwortet
 * der ai-service mit dieser Mock-Struktur. Sie ist realistisch genug, damit das
 * BPMN-Team den gesamten Prozess gegen einen echten Service testen kann — der
 * Vertrag mit der Process Engine ist identisch zur spaeteren Variante.
 *
 * <p>Die Struktur entspricht dem Schnittstellenvertrag (Stand 29.05.2026):
 * {@link Angebotspositionen} mit {@code leistungen}/{@code material}/{@code notizen}.
 *
 * <p>Wichtig: KEINE Preise (Datenschutz-Constraint, siehe {@link Position}).
 */
@ApplicationScoped
public class StubResultGenerator {

    public ErgebnisKi forErstangebot(ProcessRequest request) {
        Angebotspositionen positionen = new Angebotspositionen(
                List.of(new Position(
                        "Fliesen verlegen",
                        "Bodenfliesen im Bad verlegen, Feinsteinzeug 60x60",
                        15.0,
                        "m²")),
                List.of(new Position(
                        "Feinsteinzeug 60x60",
                        "Großformatige Bodenfliese",
                        15.0,
                        "m²")),
                List.of("Materialmengen vor Ort prüfen."));
        return new ErgebnisKi(positionen, List.of(
                "Bitte Materialfarbe und Fugenfarbe mit dem Kunden abstimmen."));
    }

    public ErgebnisKi forKorrektur(ProcessRequest request) {
        Angebotspositionen positionen = new Angebotspositionen(
                List.of(
                        new Position(
                                "Fliesen verlegen",
                                "Bodenfliesen im Bad verlegen, Feinsteinzeug 60x60",
                                15.0,
                                "m²"),
                        new Position(
                                "Sockelleisten setzen",
                                "Wurde laut Korrekturschnipsel ergänzt",
                                25.0,
                                "m")),
                List.of(
                        new Position(
                                "Feinsteinzeug 60x60",
                                "Großformatige Bodenfliese",
                                15.0,
                                "m²"),
                        new Position(
                                "Sockelleisten",
                                "Passende Sockelleisten umlaufend",
                                25.0,
                                "m")),
                List.of("Korrektur eingearbeitet — finale Mengen prüfen."));
        return new ErgebnisKi(positionen, List.of(
                "Sockelleisten ergänzt — bitte gewünschte Höhe bestätigen."));
    }
}
