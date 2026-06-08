package de.winfprojekt.craftvoice.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Strukturierte Angebotspositionen — die zentrale fachliche Datenstruktur des
 * Angebotsprozesses.
 *
 * <p>Form laut Schnittstellendokumentation (Process Engine ⇄ KI-Backend, Stand
 * 29.05.2026):
 * <pre>
 * {
 *   "leistungen": [ Position, ... ],
 *   "material":   [ Position, ... ],
 *   "notizen":    [ "...", "..." ]
 * }
 * </pre>
 *
 * <p>Wird an zwei Stellen verwendet:
 * <ul>
 *   <li><b>Input Korrektur:</b> als Feld {@code strukturierteAngebotspositionen} im
 *       {@link ProcessRequest} (der bestehende, zu ueberarbeitende Stand).</li>
 *   <li><b>Output (alle Faelle):</b> als Feld {@code strukturierteAngebotspositionen}
 *       der {@code ergebnisKI}-Message (siehe {@link ErgebnisKi}).</li>
 * </ul>
 *
 * <p>Strukturell identisch zur {@link Vorlage}; fachlich aber ein anderes Konzept
 * (Vorlage = Stammdaten des Handwerkers, hier = konkrete Angebotsdaten eines Falls),
 * daher bewusst ein eigenes Record.
 *
 * @param leistungen Liste der Leistungspositionen
 * @param material   Liste der Materialpositionen
 * @param notizen    freie Textnotizen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Angebotspositionen(
        List<Position> leistungen,
        List<Position> material,
        List<String> notizen
) {}
