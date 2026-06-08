package de.winfprojekt.craftvoice.aiservice.model;

import java.util.List;

/**
 * Ergebnis der KI-Verarbeitung. Wird vom ai-service erzeugt und per
 * {@code ergebnisKI}-Message (mit {@code businessKey}) an die Process Engine
 * korreliert.
 *
 * <p>Struktur laut Schnittstellenvertrag (Stand 29.05.2026). Die Process Engine
 * extrahiert aus diesem Objekt anschließend {@code ergebnisKI.strukturierteAngebotspositionen}:
 * <pre>
 * {
 *   "strukturierteAngebotspositionen": {
 *     "leistungen": [ Position, ... ],
 *     "material":   [ Position, ... ],
 *     "notizen":    [ "...", "..." ]
 *   },
 *   "korrekturvorschlaege": [ "Hinweis 1", "Hinweis 2" ]
 * }
 * </pre>
 *
 * <p><b>Wichtige Änderung ggü. früherem Stand:</b> {@code strukturierteAngebotspositionen}
 * ist ein Objekt ({@link Angebotspositionen}) mit {@code leistungen}/{@code material}/
 * {@code notizen} — NICHT mehr eine flache Positions-Liste.
 *
 * <p><b>Datenschutz-Constraint:</b> Die enthaltenen {@link Position}en tragen bewusst
 * KEIN {@code preis}-Feld — Preise werden der KI nicht übergeben und nicht von ihr bestimmt.
 *
 * @param strukturierteAngebotspositionen erzeugte (oder korrigierte) Positionen
 * @param korrekturvorschlaege            optionale Hinweise/Rückfragen an den Handwerker
 */
public record ErgebnisKi(
        Angebotspositionen strukturierteAngebotspositionen,
        List<String> korrekturvorschlaege
) {}
