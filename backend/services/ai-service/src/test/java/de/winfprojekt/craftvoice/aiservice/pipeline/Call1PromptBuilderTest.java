package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.model.Angebotspositionen;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;
import de.winfprojekt.craftvoice.aiservice.model.Vorlage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Tests fuer den Prompt-Aufbau ({@link Call1PromptBuilder}).
 * Sichert die wichtigsten Invarianten: Preis-Verbot im System-Prompt, getrennte
 * Erstangebot-/Korrektur-Prompts, und dass der businessKey NICHT ans Modell geht.
 */
class Call1PromptBuilderTest {

    private final Call1PromptBuilder builder = new Call1PromptBuilder(new ObjectMapper());

    @Test
    void systemPromptErstangebot_verbietetPreise() {
        String system = builder.systemPrompt(ProcessType.ERSTANGEBOT);
        assertTrue(system.contains("NIEMALS Preise"),
                "System-Prompt muss das Preis-Verbot enthalten (Datenschutz-Constraint).");
    }

    @Test
    void systemPromptKorrektur_unterscheidetSichVomErstangebot() {
        String erst = builder.systemPrompt(ProcessType.ERSTANGEBOT);
        String korr = builder.systemPrompt(ProcessType.KORREKTUR);
        assertNotEquals(erst, korr);
        assertTrue(korr.contains("ueberarbeitest"),
                "Korrektur-Prompt muss die Ueberarbeitungs-Aufgabe beschreiben.");
        assertTrue(korr.contains("NIEMALS Preise"),
                "Auch der Korrektur-Prompt muss das Preis-Verbot enthalten.");
    }

    @Test
    void userContentErstangebot_enthaeltSprachschnipsel_aberNichtBusinessKey() {
        ProcessRequest request = new ProcessRequest(
                "BK-GEHEIM-123", "prompt",
                new Vorlage(List.of(), List.of(), List.of()),
                "Im Bad neue Bodenfliesen verlegen.",
                null, null);

        String user = builder.userContent(ProcessType.ERSTANGEBOT, request);

        assertTrue(user.contains("Im Bad neue Bodenfliesen verlegen."));
        assertTrue(user.contains("sprachschnipsel"));
        assertTrue(user.contains("vorlage"));
        assertFalse(user.contains("BK-GEHEIM-123"),
                "Der businessKey (Korrelations-ID) darf nicht an das Modell gehen.");
    }

    @Test
    void userContentKorrektur_enthaeltKorrekturschnipsel() {
        ProcessRequest request = new ProcessRequest(
                "BK-2", "prompt", null, null,
                new Angebotspositionen(List.of(), List.of(), List.of()),
                "Bitte zusaetzlich Sockelleisten einplanen.");

        String user = builder.userContent(ProcessType.KORREKTUR, request);

        assertTrue(user.contains("Bitte zusaetzlich Sockelleisten einplanen."));
        assertTrue(user.contains("korrekturschnipsel"));
        assertTrue(user.contains("strukturierteAngebotspositionen"));
    }

    @Test
    void systemPrompt_enthaeltArbeitsdauerRegel_nichtSelbstSchaetzen() {
        String erst = builder.systemPrompt(ProcessType.ERSTANGEBOT);
        assertTrue(erst.contains("geschaetzteArbeitsdauerStunden"),
                "Schema muss das Stunden-Feld enthalten.");
        assertTrue(erst.contains("NIEMALS selbst"),
                "Regel muss klarstellen: KI schaetzt die Dauer nicht selbst.");

        String korr = builder.systemPrompt(ProcessType.KORREKTUR);
        assertTrue(korr.contains("geschaetzteArbeitsdauerStunden"));
    }
}
