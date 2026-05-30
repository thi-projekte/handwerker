package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Leitet aus den vorhandenen Feldern einer {@link ProcessRequest} ab, ob es sich um
 * ein Erstangebot oder eine Angebotskorrektur handelt.
 *
 * <p>Hintergrund: Der Schnittstellenvertrag (Stand 29.05.2026) verzichtet auf ein
 * explizites {@code typ}-Feld im Connector-Payload und unterscheidet die Faelle ueber
 * Feld-Anwesenheit — in genau der Reihenfolge, in der auch das BPMN-Script prueft
 * (Korrektur vor Erstangebot).
 *
 * <p>Regeln:
 * <ul>
 *   <li><b>Korrektur:</b> {@code strukturierteAngebotspositionen} UND
 *       {@code korrekturschnipsel} vorhanden.</li>
 *   <li><b>Erstangebot:</b> {@code vorlage} UND {@code sprachschnipsel} vorhanden.</li>
 *   <li><b>Sonst:</b> {@link IllegalArgumentException} — fail-fast statt raten. Das
 *       greift insbesondere auch fuer die (hier nicht unterstuetzte)
 *       Rechnungskorrektur-Variante.</li>
 * </ul>
 */
@ApplicationScoped
public class ProcessTypeDetector {

    public ProcessType determine(ProcessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ProcessRequest darf nicht null sein.");
        }

        boolean hasKorrekturFelder =
                request.strukturierteAngebotspositionen() != null
                        && request.korrekturschnipsel() != null;
        boolean hasErstangebotFelder =
                request.vorlage() != null && request.sprachschnipsel() != null;

        if (hasKorrekturFelder) {
            return ProcessType.KORREKTUR;
        }
        if (hasErstangebotFelder) {
            return ProcessType.ERSTANGEBOT;
        }

        throw new IllegalArgumentException(
                "Eingangs-Payload enthaelt weder vollstaendige Erstangebot-Felder "
                        + "(vorlage + sprachschnipsel) noch Korrektur-Felder "
                        + "(strukturierteAngebotspositionen + korrekturschnipsel). "
                        + "Rechnungskorrektur wird ueber diesen Endpoint nicht unterstuetzt. "
                        + "businessKey=" + request.businessKey());
    }
}
