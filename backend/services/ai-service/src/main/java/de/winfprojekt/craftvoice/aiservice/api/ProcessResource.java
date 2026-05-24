package de.winfprojekt.craftvoice.aiservice.api;

import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessResponse;

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
 * eines Sprachschnipsels (Erstangebot) oder einer Korrektur anstößt.
 *
 * <p><b>Aktueller Zustand (Ticket #529):</b> Nur Endpoint-Gerüst mit Acknowledge-Response.
 * Payload-Parsing (#530), Fallunterscheidung (#531), Stub-Antwort (#532) und
 * Camunda-Korrelation (#533) folgen in den nächsten Tickets.
 */
@Path("/ai")
public class ProcessResource {

    private static final Logger LOG = Logger.getLogger(ProcessResource.class);

    @POST
    @Path("/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response process(ProcessRequest request) {
        String businessKey = request != null ? request.businessKey() : null;
        LOG.infof("POST /ai/process empfangen, businessKey=%s", businessKey);

        return Response.accepted(ProcessResponse.accepted(businessKey)).build();
    }
}
