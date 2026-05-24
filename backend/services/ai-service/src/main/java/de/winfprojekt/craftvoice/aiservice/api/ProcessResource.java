package de.winfprojekt.craftvoice.aiservice.api;

import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessResponse;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;
import de.winfprojekt.craftvoice.aiservice.pipeline.ProcessTypeDetector;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

/**
 * REST-Endpoint des ai-service.
 *
 * <p>Wird vom Camunda HTTP-Connector aufgerufen, wenn ein BPMN-Prozess die KI-Verarbeitung
 * eines Sprachschnipsels (Erstangebot) oder einer Korrektur anstoesst.
 *
 * <p><b>Aktueller Zustand (Ticket #531):</b> Endpoint nimmt Payload entgegen, parst sie
 * (#530), unterscheidet Erstangebot vs. Korrektur ueber {@link ProcessTypeDetector} und
 * leitet an die jeweilige (noch leere) Handler-Methode. Stub-Antwort folgt in #532,
 * Camunda-Korrelation in #533.
 */
@Path("/ai")
public class ProcessResource {

    private static final Logger LOG = Logger.getLogger(ProcessResource.class);

    private final ProcessTypeDetector typeDetector;

    public ProcessResource(ProcessTypeDetector typeDetector) {
        this.typeDetector = typeDetector;
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
        switch (type) {
            case ERSTANGEBOT -> handleErstangebot(request);
            case KORREKTUR   -> handleKorrektur(request);
        }

        return Response.accepted(ProcessResponse.accepted(businessKey)).build();
    }

    private void handleErstangebot(ProcessRequest request) {
        // TODO #532: Stub-Antwort erzeugen, in #533 an Camunda korrelieren
    }

    private void handleKorrektur(ProcessRequest request) {
        // TODO #532: Stub-Antwort erzeugen, in #533 an Camunda korrelieren
    }

    /** Schmales Fehler-DTO fuer 400-Antworten. */
    public record ErrorResponse(String error) {}
}
