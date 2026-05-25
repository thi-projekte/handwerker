package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.AngebotsPosition;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
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
 * <p>Pro Fall gibt es leicht unterschiedliche Antworten, damit beim manuellen
 * Test der Unterschied sichtbar ist (Erstangebot = neue Positionen, Korrektur =
 * angepasste Positionen).
 *
 * <p><b>Datenschutz:</b> Kein {@code preis}-Feld in den Positionen.
 */
@ApplicationScoped
public class StubResultGenerator {

    public ErgebnisKi forErstangebot(ProcessRequest request) {
        List<AngebotsPosition> positionen = List.of(
                new AngebotsPosition(
                        "Bodenfliesen Feinsteinzeug 60x60",
                        "Liefern und verlegen im Badezimmer, matt, rektifiziert",
                        15.0, "m2"
                ),
                new AngebotsPosition(
                        "Verfugungsarbeiten",
                        "Verfugen der Bodenfliesen mit Zementfuge grau",
                        15.0, "m2"
                ),
                new AngebotsPosition(
                        "Sockelleiste passend",
                        "Sockelleiste aus gleichem Material, gehrungsgeschnitten",
                        18.0, "m"
                )
        );

        List<String> hinweise = List.of(
                "Stub-Antwort: bitte verifizieren ob Mengenangabe 15 m² zur tatsaechlichen Raumgroesse passt.",
                "Stub-Antwort: keine Materialfarbe genannt — Annahme matt/grau."
        );

        return new ErgebnisKi(positionen, hinweise);
    }

    public ErgebnisKi forKorrektur(ProcessRequest request) {
        List<AngebotsPosition> positionen = List.of(
                new AngebotsPosition(
                        "Bodenfliesen Feinsteinzeug 60x60",
                        "Liefern und verlegen im Badezimmer (unveraendert)",
                        15.0, "m2"
                ),
                new AngebotsPosition(
                        "Verfugungsarbeiten",
                        "Verfugen der Bodenfliesen mit Zementfuge grau (unveraendert)",
                        15.0, "m2"
                ),
                new AngebotsPosition(
                        "Sockelleiste passend",
                        "Sockelleiste aus gleichem Material (unveraendert)",
                        18.0, "m"
                ),
                new AngebotsPosition(
                        "Eckschienen Edelstahl",
                        "Hinzugefuegt auf Korrekturwunsch — Eckabschluesse Edelstahl gebuerstet",
                        4.0, "Stk."
                )
        );

        List<String> hinweise = List.of(
                "Stub-Antwort: Korrekturwunsch interpretiert, neue Position eingefuegt."
        );

        return new ErgebnisKi(positionen, hinweise);
    }
}
