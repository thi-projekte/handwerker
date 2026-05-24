package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Leitet aus den vorhandenen Feldern einer {@link ProcessRequest} ab, ob es sich um
 * ein Erstangebot oder eine Korrektur handelt.
 *
 * <p>Hintergrund: Die neue BPMN-Version verzichtet auf ein explizites {@code typ}-Feld
 * im Connector-Payload und unterscheidet die Faelle ueber Feld-Anwesenheit. Das ist
 * implizit und fehleranfaellig — siehe {@link ProcessType} fuer die offene Frage an
 * das BPMN-Team.
 *
 * <p>Regeln:
 * <ul>
 *   <li><b>Erstangebot:</b> {@code vorlage} UND {@code sprachschnipsel} vorhanden,
 *       {@code angebotsentwurf} und {@code korrekturschnipsel} fehlen.</li>
 *   <li><b>Korrektur:</b> {@code angebotsentwurf} UND {@code korrekturschnipsel}
 *       vorhanden.</li>
 *   <li><b>Sonst:</b> {@link IllegalArgumentException} — fail-fast statt raten.</li>
 * </ul>
 */
@ApplicationScoped
public class ProcessTypeDetector {

    public ProcessType determine(ProcessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ProcessRequest darf nicht null sein.");
        }

        boolean hasKorrekturFelder =
                request.angebotsentwurf() != null && request.korrekturschnipsel() != null;
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
                        + "(angebotsentwurf + korrekturschnipsel). businessKey="
                        + request.businessKey());
    }
}
