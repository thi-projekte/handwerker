package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Erzeugt die {@link ErgebnisKi} fuer LLM-Call 1 (Ticket #538) — der produktive Ersatz
 * fuer den {@link StubResultGenerator}.
 *
 * <p>Ablauf: System-Prompt + User-Content bauen ({@link Call1PromptBuilder}), MegaLLM
 * aufrufen ({@link MegaLlmService}), Antwort robust parsen ({@link ErgebnisKiParser}).
 *
 * <p><b>Stub-Fallback (bewusste Entscheidung):</b> Ist kein {@code MEGALLM_API_KEY}
 * gesetzt oder schlaegt der LLM-Aufruf/das Parsen fehl, faellt der Generator auf den
 * {@link StubResultGenerator} zurueck. So bleibt der Service lokal/in der Demo ohne Key
 * lauffaehig und ein LLM-Ausfall fuehrt nicht zum kompletten Prozessabbruch. Der Fallback
 * wird als WARN/ERROR geloggt.
 *
 * <p>Das Modell kommt aus {@code megallm.model.default} (Default {@code gemini-3-flash-preview},
 * Ergebnis der Modellwahl #537).
 */
@ApplicationScoped
public class LlmCall1Generator {

    private static final Logger LOG = Logger.getLogger(LlmCall1Generator.class);

    private final MegaLlmService megaLlm;
    private final Call1PromptBuilder promptBuilder;
    private final ErgebnisKiParser parser;
    private final StubResultGenerator stub;
    private final String model;

    public LlmCall1Generator(MegaLlmService megaLlm,
                             Call1PromptBuilder promptBuilder,
                             ErgebnisKiParser parser,
                             StubResultGenerator stub,
                             @ConfigProperty(name = "megallm.model.default",
                                     defaultValue = "gemini-3-flash-preview") String model) {
        this.megaLlm = megaLlm;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.stub = stub;
        this.model = model;
    }

    public ErgebnisKi forErstangebot(ProcessRequest request) {
        return generate(ProcessType.ERSTANGEBOT, request);
    }

    public ErgebnisKi forKorrektur(ProcessRequest request) {
        return generate(ProcessType.KORREKTUR, request);
    }

    private ErgebnisKi generate(ProcessType type, ProcessRequest request) {
        String businessKey = request != null ? request.businessKey() : null;

        if (!megaLlm.isConfigured()) {
            LOG.warnf("MEGALLM_API_KEY nicht gesetzt — nutze Stub-Fallback (%s, businessKey=%s).",
                    type, businessKey);
            return stubFor(type, request);
        }

        try {
            String systemPrompt = promptBuilder.systemPrompt(type);
            String userContent = promptBuilder.userContent(type, request);
            String rawOutput = megaLlm.complete(model, systemPrompt, userContent);
            ErgebnisKi ergebnis = parser.parse(rawOutput);
            LOG.infof("LLM-Call 1 erfolgreich (%s, businessKey=%s, model=%s).",
                    type, businessKey, model);
            return ergebnis;
        } catch (RuntimeException e) {
            LOG.errorf(e, "LLM-Call 1 fehlgeschlagen — nutze Stub-Fallback (%s, businessKey=%s).",
                    type, businessKey);
            return stubFor(type, request);
        }
    }

    private ErgebnisKi stubFor(ProcessType type, ProcessRequest request) {
        return type == ProcessType.KORREKTUR
                ? stub.forKorrektur(request)
                : stub.forErstangebot(request);
    }
}
