package de.winfprojekt.craftvoice.aiservice.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.CamundaCorrelationRequest;
import de.winfprojekt.craftvoice.aiservice.client.CamundaMessageClient;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessResponse;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;
import de.winfprojekt.craftvoice.aiservice.pipeline.LlmCall1Generator;
import de.winfprojekt.craftvoice.aiservice.pipeline.ProcessTypeDetector;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * REST-Endpoint des ai-service.
 *
 * <p>Wird vom Camunda HTTP-Connector aufgerufen, wenn ein BPMN-Prozess die KI-Verarbeitung
 * eines Sprachschnipsels (Erstangebot) oder einer Korrektur anstoesst.
 *
 * <p><b>Wichtig — async-Pattern wegen BPMN-Race-Condition:</b><br>
 * Der HTTP-Connector in der BPMN-SendTask blockiert die Prozessausfuehrung waehrend
 * unseres Aufrufs. Erst NACH unserer HTTP-Antwort schaltet Camunda intern auf den
 * folgenden ReceiveTask um und legt die Subscription fuer die {@code ergebnisKI}-Message
 * an. Wuerden wir die Message synchron innerhalb des Request-Handlings senden, kaeme sie
 * an bevor die Subscription existiert -> Camunda HTTP 400.
 *
 * <p>Loesung: Wir antworten der HTTP-Connector-Anfrage <b>sofort</b> mit 202, und feuern
 * die Camunda-Korrelation per {@link CompletableFuture#runAsync} im Hintergrund ab. So
 * ist die Subscription beim Eintreffen unserer Message zuverlaessig aktiv.
 *
 * <p><b>Latenz:</b> Der eigentliche LLM-Call (#538, {@link LlmCall1Generator}) kann mehrere
 * Sekunden dauern. Er laeuft daher ebenfalls im Hintergrund-Teil (nach der 202) — der
 * HTTP-Connector wird nicht blockiert und das Subscription-Timing oben bleibt gewahrt.
 *
 * <p>Aktueller Stand: Tickets #529–#534 (Geruest/Routing/Camunda) sowie #538 (echter
 * LLM-Call 1 mit Stub-Fallback) sind ueber diese Klasse abgedeckt; #541 (LLM-Call 2)
 * folgt.
 */
@Path("/ai")
public class ProcessResource {

    private static final Logger LOG = Logger.getLogger(ProcessResource.class);

    private final ProcessTypeDetector typeDetector;
    private final LlmCall1Generator llmGenerator;
    private final CamundaMessageClient camundaClient;
    private final ObjectMapper objectMapper;

    public ProcessResource(ProcessTypeDetector typeDetector,
                           LlmCall1Generator llmGenerator,
                           @RestClient CamundaMessageClient camundaClient,
                           ObjectMapper objectMapper) {
        this.typeDetector = typeDetector;
        this.llmGenerator = llmGenerator;
        this.camundaClient = camundaClient;
        this.objectMapper = objectMapper;
    }

    @POST
    @Path("/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response process(ProcessRequest request) {
        String businessKey = request != null ? request.businessKey() : null;
        LOG.infof("POST /ai/process empfangen, businessKey=%s", businessKey);

        ProcessType type;
        try {
            type = typeDetector.determine(request);
        } catch (IllegalArgumentException e) {
            LOG.warnf("Payload nicht zuordenbar: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }

        LOG.infof("Routing auf %s (businessKey=%s)", type, businessKey);

        // Schwere Arbeit (LLM-Call ~Sekunden + anschliessendes Senden) bewusst NACH der
        // HTTP-Antwort im Hintergrund: Erst nach Rueckkehr unserer 202 aktiviert Camunda
        // intern den ReceiveTask und legt die ergebnisKI-Subscription an. Synchrones
        // Verarbeiten wuerde (a) die Message u.U. vor der Subscription senden -> HTTP 400
        // und (b) den HTTP-Connector sekundenlang blockieren.
        CompletableFuture.runAsync(() -> verarbeiteUndKorreliere(type, request, businessKey))
                .exceptionally(throwable -> {
                    LOG.errorf(throwable,
                            "Asynchrone KI-Verarbeitung fehlgeschlagen (businessKey=%s)",
                            businessKey);
                    return null;
                });

        return Response.accepted(ProcessResponse.accepted(businessKey)).build();
    }

    /**
     * Erzeugt das KI-Ergebnis (LLM-Call 1 mit Stub-Fallback), serialisiert es und
     * korreliert es an Camunda. Laeuft im Hintergrund-Thread (siehe {@link #process}),
     * darf also blockieren und antwortet dem Aufrufer nicht mehr per HTTP — Fehler werden
     * nur geloggt.
     */
    private void verarbeiteUndKorreliere(ProcessType type, ProcessRequest request, String businessKey) {
        ErgebnisKi ergebnis = switch (type) {
            case ERSTANGEBOT -> llmGenerator.forErstangebot(request);
            case KORREKTUR   -> llmGenerator.forKorrektur(request);
        };

        String ergebnisJson;
        try {
            ergebnisJson = objectMapper.writeValueAsString(ergebnis);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Konnte ergebnisKI nicht zu JSON serialisieren (businessKey=%s)",
                    businessKey);
            return;
        }

        sendErgebnisKiToCamunda(businessKey, ergebnisJson);
    }

    private void sendErgebnisKiToCamunda(String businessKey, String ergebnisJson) {
        CamundaCorrelationRequest correlation =
                CamundaCorrelationRequest.ergebnisKI(businessKey, ergebnisJson);

        try (Response response = camundaClient.correlate(correlation)) {
            int status = response.getStatus();
            if (status >= 400) {
                throw new RuntimeException(
                        "Camunda hat Korrelation abgelehnt: HTTP " + status);
            }
            LOG.infof("ergebnisKI-Message erfolgreich an Camunda korreliert "
                            + "(businessKey=%s, HTTP %d)", businessKey, status);
        }
    }

    /** Schmales Fehler-DTO fuer 400/500-Antworten. */
    public record ErrorResponse(String error) {}
}
