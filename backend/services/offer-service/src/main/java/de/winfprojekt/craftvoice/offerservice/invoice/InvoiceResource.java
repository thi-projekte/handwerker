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
     * @param request Anfrageobjekt mit angebotId
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
     * Gibt die Rechnung mit der angegebenen ID zurück.
     *
     * @param id ID der gesuchten Rechnung
     * @return HTTP 200 mit der Rechnung inkl. Positionen, oder 404 wenn nicht gefunden
     */
    @GET
    @Path("/{id}")
    public Response getInvoiceById(@PathParam("id") Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return Response.ok(response).build();
    }
}
