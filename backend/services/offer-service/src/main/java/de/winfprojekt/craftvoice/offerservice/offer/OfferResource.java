package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.offer.dto.*;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import jakarta.annotation.security.PermitAll;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * REST-Ressource zur Verwaltung von Angeboten.
 *
 * Stellt Endpunkte zum Erstellen und Abrufen von Angeboten bereit.
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class OfferResource {

    @Inject
    OfferService offerService;

    @Inject
    JsonWebToken jwt;

    /**
     * Erstellt ein neues Angebot auf Basis der übergebenen Daten.
     *
     * @param request Anfrageobjekt mit den Daten zur Angebotserstellung
     * @return HTTP-Response mit dem erzeugten Angebot und Statuscode 201
     */
    @POST
    @Path("/offers")
    @RolesAllowed({"OWNER"})
    public Response createOffer(@Valid CreateOfferRequest request) {
        String userID = jwt.getSubject();
        OfferResponse response = offerService.createOffer(request, userID);

        return Response.status(201)
                .entity(response)
                .build();
    }

    /**
     * Verarbeitet das KI-Ergebnis für ein bestimmtes Angebot, adressiert über den
     * businessKey.
     *
     * @param businessKey Business-Key des Angebots (von der Process Engine bekannt)
     * @param request     Anfrageobjekt mit dem KI-Ergebnis
     * @return HTTP-Response mit Statuscode 200 bei Erfolg
     */
    @POST
    @Path("/angebote/{businessKey}/ki-ergebnis")
    @RolesAllowed("OWNER")
    public Response processAiResult(@PathParam("businessKey") String businessKey, @Valid OfferChangesRequest request) {
        String userId = jwt.getSubject();
        Offer offer = offerService.findOwnOfferOrThrow(businessKey, userId);

        offerService.initializeOrUpdateOfferFromAiOrFrontend(offer.businessKey, request);

        return Response.status(200).build();
    }

    /**
     * Verarbeitet die Änderungen des Handwerkers im Frontend für ein bestimmtes
     * Angebot.
     *
     * @param id      ID des Angebots
     * @param request Anfrageobjekt mit dem KI-Ergebnis
     * @return HTTP-Response mit Statuscode 200 bei Erfolg
     */
    @POST
    @Path("/angebote/{businessKey}/positionen")
    @RolesAllowed({"OWNER"})
    public Response processOfferChanges(@PathParam("businessKey") String businessKey, @Valid OfferChangesRequest request) {
        String userId = jwt.getSubject();
        Offer offer = offerService.findOwnOfferOrThrow(businessKey, userId);

        offerService.initializeOrUpdateOfferFromAiOrFrontend(offer.businessKey, request);

        return Response.status(200).build();
    }

    /**
     * Verarbeitet die manuell eingetragene Arbeitsdauer des Handwerkers.
     * Legt ggf. eine Arbeitszeit-Position an und informiert die Process Engine.
     *
     * @param id      ID des Angebots (muss im Status KI_FERTIG sein)
     * @param request Arbeitsstunden-Eingabe des Handwerkers
     * @return HTTP-Response 200 mit dem aktualisierten Angebot
     */
    @POST
    @Path("/angebote/{businessKey}/arbeitsstunden")
    @RolesAllowed({"OWNER"})
    public Response setArbeitsstunden(@PathParam("businessKey") String businessKey, @Valid SetArbeitsstundenRequest request) {
        String userId = jwt.getSubject();
        OfferResponse response = offerService.setArbeitsstunden(businessKey, userId, request);
        return Response.ok(response).build();
    }

    /**
     * Gibt eine Liste aller Angebote zurück, sortiert nach Erstellungsdatum
     * absteigend (neueste zuerst).
     *
     * @return HTTP-Response mit Statuscode 200 und der Liste aller Angebote
     */
    @GET
    @Path("/offers")
    @RolesAllowed({"OWNER"})
    public Response getAllOffers() {
        String userID = jwt.getSubject();
        return Response.ok(offerService.getAllOffersSorted(userID)).build();
    }

    /**
     * Gibt das Angebot mit der angegebenen ID zurück, falls es existiert.
     *
     * @param id ID des gesuchten Angebots
     * @return HTTP-Response mit Statuscode 200 und dem gefundenen Angebot, oder
     *         Statuscode 404
     */
    @GET
    @Path("/offers/{businessKey}")
    @RolesAllowed({"OWNER"})
    public Response getOfferById(@PathParam("businessKey") String businessKey) {
        String userId = jwt.getSubject();
        // OS-2: Laden + DTO-Mapping laufen in der @Transactional-Service-Methode,
        // damit die Lazy-Collections innerhalb einer aktiven Session initialisiert werden.
        OfferResponse response = offerService.getOfferByBusinessKey(businessKey, userId);
        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(response).build();
    }

    /**
     * Endpunkt zur Annahme oder Ablehnung eines Angebots durch den Kunden über
     * einen Token.
     * Dieser Endpunkt ist öffentlich zugänglich.
     *
     * @param token   Der eindeutige Annahme-Token des Angebots
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

    /**
     * Endpunkt zur Annahme eines Angebots durch den Handwerker nach KI-Durchlauf.
     * Dieser Endpunkt ist öffentlich zugänglich.
     * 
     * @param id Angebots-ID des angenommenen Angebots
     * @return HTTP-Response mit "204 No Content status code" im Happy Path oder
     *         alternativ eine Fehlermeldung
     */
    @POST
    @Path("/offers/{businessKey}/review/approve")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"OWNER"})
    public Response acceptAiResult(@PathParam("businessKey") String businessKey) {
        String userId = jwt.getSubject();
        offerService.acceptAiResult(businessKey, userId);
        return Response.noContent().build();
    }

    /**
     * Setzt den Status eines Angebots auf VERSANDBEREIT.
     * Wird vom document-service aufgerufen, nachdem das PDF-Angebot erfolgreich erstellt wurde.
     *
     * @param businessKey Business-Key des Angebots
     * @return HTTP-Response mit Statuscode 200 bei Erfolg
     */
    @POST
    @Path("/angebote/{businessKey}/versandbereit")
    @Consumes(MediaType.WILDCARD)
    public Response setStatusVersandbereit(@PathParam("businessKey") String businessKey) {
        offerService.setStatusVersandbereit(businessKey);
        return Response.ok().build();
    }

    /**
     * Setzt den Status eines Angebots auf VERSENDET.
     * Wird vom document-service aufgerufen, nachdem das Angebot erfolgreich versendet wurde.
     *
     * @param businessKey Business-Key des Angebots
     * @return HTTP-Response mit Statuscode 200 bei Erfolg
     */
    @POST
    @Path("/angebote/{businessKey}/versendet")
    @Consumes(MediaType.WILDCARD)
    public Response setStatusVersendet(@PathParam("businessKey") String businessKey) {
        offerService.setStatusVersendet(businessKey);
        return Response.ok().build();
    }

    /**
     * Ändert den Status eines Angebots manuell durch den Handwerker.
     * Dies ist nur erlaubt, wenn das Angebot bereits versendet wurde (Status VERSENDET).
     * Erlaubte Zielstatus sind ANGENOMMEN oder ABGELEHNT.
     *
     * @param businessKey Business-Key des Angebots
     * @param request     Request-DTO mit dem gewünschten Zielstatus
     * @return HTTP-Response 200 bei Erfolg
     */
    @PUT
    @Path("/offers/{businessKey}/status")
    @RolesAllowed({"OWNER"})
    public Response updateOfferStatusManually(
            @PathParam("businessKey") String businessKey,
            @Valid UpdateOfferStatusRequest request) {
        String userId = jwt.getSubject();
        offerService.updateOfferStatusManually(businessKey, request.status, userId);
        return Response.ok().build();
    }
}