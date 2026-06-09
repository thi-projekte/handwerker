package de.winfprojekt.craftvoice.offerservice.offer;

import de.winfprojekt.craftvoice.offerservice.offer.dto.DashboardStats;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST-Ressource für Dashboard-Aggregationsdaten.
 *
 * <p>Liefert alle Kennzahlen in einem einzigen Request, damit das Frontend
 * keine mehreren Einzelabfragen stellen muss.
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    /**
     * Gibt alle aggregierten Dashboard-Kennzahlen zurück.
     *
     * @return HTTP 200 mit {@link DashboardStats}; alle Felder sind immer befüllt
     */
    @GET
    public Response getDashboard() {
        DashboardStats stats = dashboardService.getDashboardStats();
        return Response.ok(stats).build();
    }
}
