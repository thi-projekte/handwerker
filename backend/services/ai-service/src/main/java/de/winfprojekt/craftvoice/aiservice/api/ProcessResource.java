package de.winfprojekt.craftvoice.aiservice.api;

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

import org.jboss.logging.Logger;

/**
 * REST-Endpoint des ai-service.
 *
 * <p>Wird vom Camunda HTTP-Connector aufgerufen, wenn ein BPMN-Prozess die KI-Verarbeitung
 * eines Sprachschnipsels (Erstangebot) oder einer Korrektur anstoesst.
 *
 * <p><b>Aktueller Zustand (Ticket #532):</b> Endpoint nimmt Payload entgegen, parst sie
 * (#530), unterscheidet Erstangebot vs. Korrektur ueber {@link ProcessTypeDetector}
 * (#531) und erzeugt eine Stub-Antwort ueber {@link StubResultGenerator} (#532).
 * Die Korrelation der Stub-Antwort als Camunda-Message folgt in #533 — aktuell wird das
 * Ergebnis nur geloggt.
 */
@Path("/ai")
public class ProcessResource {

    private static final Logger LOG = Logger.getLogger(ProcessResource.class);

    private final ProcessTypeDetector typeDetector;
    private final StubResultGenerator stubGenerator;

    public ProcessResource(ProcessTypeDetector typeDetector,
                           StubResultGenerator stubGenerator) {
        this.typeDetector = typeDetector;
        this.stubGenerator = stubGenerator;
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

        // TODO #533: ergebnisKI als Message an Camunda korrelieren (per businessKey)
        LOG.infof("Stub-Ergebnis erzeugt: %d Positionen, %d Korrekturvorschlaege (businessKey=%s)",
                ergebnis.strukturierteAngebotspositionen().size(),
                ergebnis.korrekturvorschlaege().size(),
                businessKey);

        return Response.accepted(ProcessResponse.accepted(businessKey)).build();
    }

    /** Schmales Fehler-DTO fuer 400-Antworten. */
    public record ErrorResponse(String error) {}
}
