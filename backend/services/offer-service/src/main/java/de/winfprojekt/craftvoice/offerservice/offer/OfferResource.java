package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.offer.dto.AiResultRequest;
import de.winfprojekt.craftvoice.offerservice.offer.dto.CreateOfferRequest;
import jakarta.ws.rs.PathParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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

        Offer offer = offerService.createOffer(request);

        return Response.status(201)
                .entity(offer)
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
}