package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.offer.dto.AiResultRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferResponse;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.OfferAcceptanceResponse;
import jakarta.annotation.security.PermitAll;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;

/**
 * REST-Ressource zur Verwaltung von Angeboten.
 *
 * Stellt Endpunkte zum Erstellen und Abrufen von Angeboten bereit.
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OfferResource {

    @Inject
    OfferService offerService;

    /**
     * Erstellt ein neues Angebot auf Basis der übergebenen Daten.
     *
     * @param request Anfrageobjekt mit den Daten zur Angebotserstellung
     * @return HTTP-Response mit dem erzeugten Angebot und Statuscode 201
     */
    @POST
    @Path("/offers")
    public Response createOffer(@Valid CreateOfferRequest request) {

        OfferResponse response = offerService.createOffer(request);

        return Response.status(201)
                .entity(response)
                .build();
    }

    /**
     * Verarbeitet das KI-Ergebnis für ein bestimmtes Angebot.
     *
     * @param id ID des Angebots
     * @param request Anfrageobjekt mit dem KI-Ergebnis
     * @return HTTP-Response mit Statuscode 200 bei Erfolg
     */
    @POST
    @Path("/angebote/{id}/ki-ergebnis")
    public Response processAiResult(@PathParam("id") Long id, @Valid AiResultRequest request) {

        offerService.processAiResult(id, request);

        return Response.status(200).build();
    }

    /**
     * Gibt eine Liste aller Angebote zurück, sortiert nach Erstellungsdatum absteigend (neueste zuerst).
     *
     * @return HTTP-Response mit Statuscode 200 und der Liste aller Angebote
     */
    @GET
    @Path("/offers")
    public Response getAllOffers() {
        return Response.ok(offerService.getAllOffersSorted()).build();
    }

    /**
     * Gibt das Angebot mit der angegebenen ID zurück, falls es existiert.
     *
     * @param id ID des gesuchten Angebots
     * @return HTTP-Response mit Statuscode 200 und dem gefundenen Angebot, oder Statuscode 404
     */
    @GET
    @Path("/offers/{id}")
    public Response getOfferById(@PathParam("id") Long id) {
        OfferResponse response = offerService.getOfferById(id);
        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(response).build();
    }

    /**
     * Endpunkt zur Annahme oder Ablehnung eines Angebots durch den Kunden über einen Token.
     * Dieser Endpunkt ist öffentlich zugänglich.
     *
     * @param token Der eindeutige Annahme-Token des Angebots
     * @param request Die Kundenentscheidung (angenommen / abgelehnt)
     * @return HTTP-Response 200 mit Bestätigungsantwort oder Fehlermeldung
     */
    @POST
    @Path("/angebote/annahme/{token}")
    @PermitAll
    public Response acceptOrRejectOffer(@PathParam("token") String token, @Valid OfferAcceptanceRequest request) {
        OfferAcceptanceResponse response = offerService.acceptOrRejectOffer(token, request);
        return Response.ok(response).build();
    }
}