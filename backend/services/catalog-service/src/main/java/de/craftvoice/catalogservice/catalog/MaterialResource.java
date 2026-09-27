package de.craftvoice.catalogservice.catalog;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.nio.file.Files;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

import io.quarkus.security.Authenticated;

@Path("/catalog/material")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialResource {

    @Inject
    MaterialService service;

    @Inject
    CsvMaterialParser csvMaterialParser;

    @Inject
    JsonWebToken jwt;

    @Context
    HttpHeaders httpHeaders;

    /** Header, ueber den ein technischer Caller (Rolle process-engine) den Ziel-Handwerker angibt. */
    private static final String HANDWERKER_HEADER = "X-Handwerker-Id";

    /** Catalog-Client-Rolle, die einen technischen Service-Aufruf (z.B. ai-service) kennzeichnet. */
    private static final String TECHNICAL_ROLE = "process-engine";

    @GET
    public List<MaterialResponse> getAll() {
        return service.getAll(ownerId())
                .stream()
                .map(MaterialResponse::fromEntity)
                .toList();
    }

    @GET
    @Path("/{id}")
    public MaterialResponse getById(@PathParam("id") UUID id) {
        Material material = service.getById(id);

        if (!material.ownerId.equals(ownerId())) {
            throw new NotFoundException("Material not found");
        }

        return MaterialResponse.fromEntity(material);
    }

    @POST
    public MaterialResponse createManual(DatanormMaterialDto dto) {
        return MaterialResponse.fromEntity(service.createManual(dto, ownerId()));
    }

    @PUT
    @Path("/{id}")
    public MaterialResponse update(@PathParam("id") UUID id, DatanormMaterialDto dto) {
        return MaterialResponse.fromEntity(service.update(id, dto, ownerId()));
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") UUID id) {
        service.delete(id, ownerId());
    }

    @POST
    @Path("/import/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public int importCsv(@RestForm("file") FileUpload file) {
        try {
            List<DatanormMaterialDto> materials =
                    csvMaterialParser.parse(
                            Files.newInputStream(file.uploadedFile())
                    );

            return service.importFromCsv(materials, ownerId());
        } catch (Exception e) {
            throw new RuntimeException("CSV Import fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    @GET
    @Path("/search")
    public MaterialSearchResultResponse search(
            @QueryParam("q") String query,
            @QueryParam("limit") Integer limit
    ) {
        return service.search(query, limit, ownerId());
    }

    @POST
    @Path("/import/datanorm")
    public MaterialResponse importFromDatanorm(DatanormMaterialDto dto) {
        return MaterialResponse.fromEntity(service.importFromDatanorm(dto, ownerId()));
    }

    /**
     * Liefert die ownerId, nach der Material gefiltert wird.
     *
     * <p>Normalfall (User-Login): die Keycloak-ID des eingeloggten Handwerkers ({@code jwt.getSubject()}).
     *
     * <p>Technischer Caller (z.B. ai-service, Rolle {@code process-engine}): hat KEIN User-Token mit
     * dem Handwerker als Subject, sondern ein technisches Token. Er gibt den Ziel-Handwerker daher
     * ueber den Header {@code X-Handwerker-Id} an. Dieser Header wird NUR vertraut, wenn der Caller
     * die Rolle {@code process-engine} traegt — sonst koennte jeder eine fremde ownerId vortaeuschen.
     */
    private String ownerId() {
        if (hasTechnicalRole()) {
            String handwerkerId = httpHeaders.getHeaderString(HANDWERKER_HEADER);
            if (handwerkerId != null && !handwerkerId.isBlank()) {
                return handwerkerId;
            }
            throw new BadRequestException(
                    "Header " + HANDWERKER_HEADER + " fehlt fuer technischen Aufruf (Rolle "
                            + TECHNICAL_ROLE + ").");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new NotAuthorizedException("Missing JWT subject");
        }
        return subject;
    }

    /**
     * Prueft, ob das Token die catalog-Client-Rolle {@code process-engine} traegt. Bei Keycloak
     * liegen Client-Rollen unter {@code resource_access.catalog.roles} (NICHT {@code realm_access}),
     * daher hier explizit aus dem Claim gelesen statt ueber das Quarkus-Default-Rollenmapping
     * (das auf die OIDC-client-id {@code mein-frontend} zeigt).
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