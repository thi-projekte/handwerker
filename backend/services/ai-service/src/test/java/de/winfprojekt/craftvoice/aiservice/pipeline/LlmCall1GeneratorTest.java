package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.MegaLlmClient;
import de.winfprojekt.craftvoice.aiservice.model.Angebotspositionen;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.Vorlage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit-Test fuer den Stub-Fallback des {@link LlmCall1Generator}.
 *
 * <p>Ohne {@code MEGALLM_API_KEY} (hier: leerer Key im {@link MegaLlmService}) darf der
 * Generator den MegaLLM-Client gar nicht erst aufrufen, sondern muss das deterministische
 * Stub-Ergebnis liefern. Das ist genau der Pfad, auf dem auch die @QuarkusTest-Endpoint-
 * Tests ohne Key gruen bleiben.
 */
class LlmCall1GeneratorTest {

    private MegaLlmClient client;
    private LlmCall1Generator generator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        client = Mockito.mock(MegaLlmClient.class);
        // Kein API-Key (leeres Optional) -> isConfigured() == false -> Stub-Fallback
        MegaLlmService megaLlm = new MegaLlmService(client, Optional.empty(), Optional.empty());
        generator = new LlmCall1Generator(
                megaLlm,
                new Call1PromptBuilder(objectMapper),
                new ErgebnisKiParser(objectMapper),
                new StubResultGenerator(),
                "gemini-3-flash-preview");
    }

    @Test
    void ohneApiKey_erstangebot_liefertStubUndRuftLlmNicht() {
        ProcessRequest request = new ProcessRequest(
                "BK-1", "prompt",
                new Vorlage(List.of(), List.of(), List.of()),
                "Im Bad Fliesen verlegen.", null, null);

        ErgebnisKi result = generator.forErstangebot(request);

        assertNotNull(result);
        assertEquals(1, result.strukturierteAngebotspositionen().leistungen().size());
        assertEquals("Fliesen verlegen",
                result.strukturierteAngebotspositionen().leistungen().get(0).bezeichnung());
        verifyNoInteractions(client);
    }

    @Test
    void ohneApiKey_korrektur_liefertStub() {
        ProcessRequest request = new ProcessRequest(
                "BK-2", "prompt", null, null,
                new Angebotspositionen(List.of(), List.of(), List.of()),
                "Sockelleisten ergaenzen.");

        ErgebnisKi result = generator.forKorrektur(request);

        // Stub-Korrektur ergaenzt eine zweite Leistung (Sockelleisten).
        assertEquals(2, result.strukturierteAngebotspositionen().leistungen().size());
        verifyNoInteractions(client);
    }
}
