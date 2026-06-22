package de.winfprojekt.craftvoice.offerservice.invoice;

import de.winfprojekt.craftvoice.offerservice.invoice.dto.CreateInvoiceRequest;
import de.winfprojekt.craftvoice.offerservice.invoice.dto.InvoiceResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST-Ressource zur Verwaltung von Rechnungen.
 *
 * <p>Stellt Endpunkte zum Erstellen und Abrufen von Rechnungen bereit.
 * PDF-Rendering übernimmt der document-service — dieser Endpunkt erzeugt nur den Datensatz.
 */
@Path("/rechnungen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InvoiceResource {

    @Inject
    InvoiceService invoiceService;

    /**
     * Erstellt eine neue Rechnung aus einem Angebot mit Status ANGENOMMEN.
     *
     * @param request Anfrageobjekt mit businessKey
     * @return HTTP 201 mit der erzeugten Rechnung, oder 404/409 bei Fehlern
     */
    @POST
    public Response createInvoice(@Valid CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    /**
     * Gibt eine Liste aller Rechnungen zurück, sortiert nach Erstellungsdatum absteigend.
     *
     * @return HTTP 200 mit der Liste aller Rechnungen
     */
    @GET
    public Response getAllInvoices() {
        List<InvoiceResponse> invoices = invoiceService.getAllInvoicesSorted();
        return Response.ok(invoices).build();
    }

    /**
     * Gibt die Rechnung mit der angegebenen internen ID zurück.
     *
     * Für den normalen Frontend-Einsatz wird {@code GET /angebot/{businessKey}}
     * bevorzugt, da das Frontend in der Regel keinen Zugriff auf interne DB-IDs hat.
     *
     * @param id interne DB-ID der gesuchten Rechnung
     * @return HTTP 200 mit der Rechnung inkl. Positionen, oder 404 wenn nicht gefunden
     */
    @GET
    @Path("/{id}")
    public Response getInvoiceById(@PathParam("id") Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return Response.ok(response).build();
    }

    /**
     * Gibt die Rechnung zum zugehörigen Angebot anhand des businessKeys zurück.
     *
     * Dies ist der bevorzugte Endpunkt für das Frontend, da das Frontend
     * ausschließlich mit dem businessKey des Angebots arbeitet.
     *
     * @param businessKey businessKey des Angebots
     * @return HTTP 200 mit der Rechnung, oder 404 wenn keine Rechnung für dieses Angebot existiert
     */
    @GET
    @Path("/angebot/{businessKey}")
    public Response getInvoiceByOfferBusinessKey(@PathParam("businessKey") String businessKey) {
        InvoiceResponse response = invoiceService.getInvoiceByOfferBusinessKey(businessKey);
        return Response.ok(response).build();
    }

    /**
     * Erstellt den Rechnungsentwurf für ein angenommenes Angebot und sendet ihn
     * direkt an die Process Engine zurück.
     *
     * <p>Dieser Endpunkt wird ausschließlich von der Process Engine aufgerufen,
     * nachdem sie die Nachricht "angebotAngenommen" empfangen hat.
     * Nach der Erstellung korreliert der Service die Nachricht "rechnungsentwurf"
     * zurück an die PE, die dann den Document Service zur PDF-Generierung aufruft.
     *
     * @param businessKey businessKey des Angebots
     * @return HTTP 204 No Content bei Erfolg
     */
    @POST
    @Path("/{businessKey}/erstellen")
    @Consumes(MediaType.WILDCARD)
    public Response createInvoiceForPe(@PathParam("businessKey") String businessKey) {
        invoiceService.createInvoiceAndNotifyPe(businessKey);
        return Response.noContent().build();
    }
}
