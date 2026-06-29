package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.Angebotspositionen;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.Position;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Erzeugt eine fest verdrahtete {@link ErgebnisKi}-Antwort als <b>Fallback</b>, wenn die echte
 * LLM-Pipeline (LLM-Call 1) nicht zur Verfuegung steht — also kein {@code MEGALLM_API_KEY}
 * gesetzt ist oder der LLM-Aufruf/das Parsen fehlschlaegt (siehe {@link LlmCall1Generator}).
 *
 * <p>Inhaltlich ein typisches <b>Wallbox-Installationsangebot</b> (Wallbox, Starkstrom- und
 * Netzwerkkabel, Leitungsschutzschalter) plus 4 Stunden Arbeitszeit. So bleibt der Notnagel
 * nah am haeufigsten Demo-/Testfall und wirkt nicht fachfremd, falls er live einspringen muss.
 * Da LLM-Call 2 ({@link Call2Selector}) auch auf dem Stub-Ergebnis laeuft, ergaenzt der Katalog
 * — sofern erreichbar — sogar echte Produkt-IDs und Preise zu diesen Materialpositionen.
 *
 * <p>Die Struktur entspricht dem Schnittstellenvertrag: {@link Angebotspositionen} mit
 * {@code leistungen}/{@code material}/{@code notizen}. Die Arbeitszeit wird ueber
 * {@code geschaetzteArbeitsdauerStunden} (4 h) gesetzt — der offer-service rechnet daraus mit
 * dem Stundensatz eine Arbeitszeit-Position.
 *
 * <p>Wichtig: KEINE Preise (Datenschutz-Constraint, siehe {@link Position}). Die Kabel-Mengen
 * sind grobe Richtwerte und vor Ort zu pruefen.
 */
@ApplicationScoped
public class StubResultGenerator {

    /** Lennard-Vorgabe fuer die Demo: der Stub setzt immer 4 Stunden Arbeitszeit. */
    private static final double STUB_ARBEITSDAUER_STUNDEN = 4.0;

    public ErgebnisKi forErstangebot(ProcessRequest request) {
        Angebotspositionen positionen = new Angebotspositionen(
                List.of(),
                List.of(
                        new Position(
                                "Wallbox",
                                "Wallbox zum Laden von Elektrofahrzeugen.",
                                1.0,
                                "Stück"),
                        new Position(
                                "Starkstromkabel NYM-J 5x6mm²",
                                "Starkstrom-Zuleitung für die Versorgung der Wallbox.",
                                10.0,
                                "m"),
                        new Position(
                                "Netzwerkkabel Cat 7",
                                "Datenleitung zur Anbindung der Wallbox.",
                                10.0,
                                "m"),
                        new Position(
                                "Leitungsschutzschalter C16",
                                "Leitungsschutzschalter zur Absicherung der Wallbox im Verteiler.",
                                1.0,
                                "Stück")),
                List.of("Leitungslängen vor Ort prüfen."));
        return new ErgebnisKi(
                positionen,
                List.of("Wallbox-Modell und Ladeleistung mit dem Kunden abstimmen."),
                STUB_ARBEITSDAUER_STUNDEN);
    }

    public ErgebnisKi forKorrektur(ProcessRequest request) {
        Angebotspositionen positionen = new Angebotspositionen(
                List.of(),
                List.of(
                        new Position(
                                "Wallbox",
                                "Wallbox zum Laden von Elektrofahrzeugen.",
                                1.0,
                                "Stück"),
                        new Position(
                                "Starkstromkabel NYM-J 5x6mm²",
                                "Starkstrom-Zuleitung für die Versorgung der Wallbox.",
                                10.0,
                                "m"),
                        new Position(
                                "Netzwerkkabel Cat 7",
                                "Datenleitung zur Anbindung der Wallbox.",
                                10.0,
                                "m"),
                        new Position(
                                "Leitungsschutzschalter C16",
                                "Leitungsschutzschalter zur Absicherung der Wallbox im Verteiler.",
                                1.0,
                                "Stück"),
                        new Position(
                                "FI-Schutzschalter Typ B",
                                "Laut Korrekturschnipsel ergänzt — allstromsensitiver Personenschutz für die Wallbox.",
                                1.0,
                                "Stück")),
                List.of("Korrektur eingearbeitet — finale Mengen vor Ort prüfen."));
        return new ErgebnisKi(
                positionen,
                List.of("Zusätzlichen FI-Schutzschalter Typ B ergänzt — bitte bestätigen."),
                STUB_ARBEITSDAUER_STUNDEN);
    }
}
