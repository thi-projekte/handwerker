package de.winfprojekt.craftvoice.aiservice.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.CamundaCorrelationRequest;
import de.winfprojekt.craftvoice.aiservice.client.CamundaMessageClient;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessResponse;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;
import de.winfprojekt.craftvoice.aiservice.pipeline.ProcessTypeDetector;
import de.winfprojekt.craftvoice.aiservice.pipeline.StubResultGenerator;

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
 * <p>Aktueller Stand: Tickets #529–#534 sind ueber diese Klasse abgedeckt. Die echte
 * LLM-Pipeline (#538/#541) ersetzt spaeter den {@link StubResultGenerator}.
 */
@Path("/ai")
public class ProcessResource {

    private static final Logger LOG = Logger.getLogger(ProcessResource.class);

    private final ProcessTypeDetector typeDetector;
    private final StubResultGenerator stubGenerator;
    private final CamundaMessageClient camundaClient;
    private final ObjectMapper objectMapper;

    public ProcessResource(ProcessTypeDetector typeDetector,
                           StubResultGenerator stubGenerator,
                           @RestClient CamundaMessageClient camundaClient,
                           ObjectMapper objectMapper) {
        this.typeDetector = typeDetector;
        this.stubGenerator = stubGenerator;
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
        ErgebnisKi ergebnis = switch (type) {
            case ERSTANGEBOT -> stubGenerator.forErstangebot(request);
            case KORREKTUR   -> stubGenerator.forKorrektur(request);
        };

        String ergebnisJson;
        try {
            ergebnisJson = objectMapper.writeValueAsString(ergebnis);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Konnte ergebnisKI nicht zu JSON serialisieren (businessKey=%s)",
                    businessKey);
            return Response.serverError()
                    .entity(new ErrorResponse("Interner Fehler bei der Ergebnis-Serialisierung"))
                    .build();
        }

        // Fire-and-forget: nach Rueckkehr unserer HTTP-Antwort aktiviert Camunda
        // intern den ReceiveTask und legt die Subscription an. Unser asynchroner
        // Send hat dann eine passende Wartestelle.
        CompletableFuture.runAsync(() -> sendErgebnisKiToCamunda(businessKey, ergebnisJson))
                .exceptionally(throwable -> {
                    LOG.errorf(throwable,
                            "Asynchrone Camunda-Korrelation fehlgeschlagen (businessKey=%s)",
                            businessKey);
                    return null;
                });

        return Response.accepted(ProcessResponse.accepted(businessKey)).build();
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
