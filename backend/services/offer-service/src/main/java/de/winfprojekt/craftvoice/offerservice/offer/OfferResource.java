package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.offer.dto.*;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import jakarta.annotation.security.PermitAll;

import jakarta.annotation.security.RolesAllowed;
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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
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

    @Context
    HttpHeaders httpHeaders;

    /** Header, über den ein technischer Caller (Rolle process-engine) den Ziel-Handwerker angibt. */
    private static final String HANDWERKER_HEADER = "X-Handwerker-Id";
    /** Client-Rolle, die einen technischen Service-Aufruf (z.B. PE/ai-service) kennzeichnet. */
    private static final String TECHNICAL_ROLE = "process-engine";

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
    @Authenticated   // statt @RolesAllowed("OWNER"): technischer Caller (PE) wird in resolveHandwerkerId() geprüft
    public Response processAiResult(@PathParam("businessKey") String businessKey, @Valid OfferChangesRequest request) {
        String userId = resolveHandwerkerId();
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
     * Liefert die Handwerker-ID (= ownerId des Angebots).
     *
     * <p>Normalfall (User-Login): die Keycloak-ID des eingeloggten Handwerkers ({@code jwt.getSubject()}).
     *
     * <p>Technischer Caller (PE/ai-service, Rolle {@code process-engine}): hat KEIN User-Token mit dem
     * Handwerker als Subject, sondern ein technisches Token. Der Ziel-Handwerker kommt daher über den
     * Header {@code X-Handwerker-Id}. Dieser Header wird NUR vertraut, wenn der Caller die Rolle
     * {@code process-engine} trägt — sonst könnte jeder eine fremde Handwerker-ID vortäuschen.
     * (Gleiches Muster wie catalog-service {@code MaterialResource.ownerId()}.)
     */
    private String resolveHandwerkerId() {
        if (hasTechnicalRole()) {
            String handwerkerId = httpHeaders.getHeaderString(HANDWERKER_HEADER);
            if (handwerkerId != null && !handwerkerId.isBlank()) {
                return handwerkerId;
            }
            throw new BadRequestException(
                    "Header " + HANDWERKER_HEADER + " fehlt für technischen Aufruf (Rolle "
                            + TECHNICAL_ROLE + ").");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new NotAuthorizedException("Missing JWT subject");
        }
        return subject;
    }

    /**
     * Prüft, ob das Token die Rolle {@code process-engine} trägt. Sie ist eine catalog-Client-Rolle
     * und liegt daher unter {@code resource_access.catalog.roles} (NICHT realm_access und NICHT im
     * offer-service-Role-Claim-Path), darum wird sie hier direkt aus dem Claim gelesen.
     */
    private boolean hasTechnicalRole() {
        Object resourceAccess = jwt.getClaim("resource_access");
        if (!(resourceAccess instanceof JsonObject ra)) {
            return false;
        }
        JsonObject catalog = ra.getJsonObject("catalog");
        if (catalog == null) {
            return false;
        }
        JsonArray roles = catalog.getJsonArray("roles");
        if (roles == null) {
            return false;
        }
        for (JsonValue role : roles) {
            if (role.getValueType() == JsonValue.ValueType.STRING
                    && TECHNICAL_ROLE.equals(((jakarta.json.JsonString) role).getString())) {
                return true;
            }
        }
        return false;
    }

}