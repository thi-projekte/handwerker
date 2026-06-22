package de.winfprojekt.craftvoice.offerservice.dashboard;

import de.winfprojekt.craftvoice.offerservice.dashboard.dto.DashboardStats;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * REST-Ressource für Dashboard-Aggregationsdaten.
 *
 * <p>Liefert alle Kennzahlen in einem einzigen Request, damit das Frontend
 * keine mehreren Einzelabfragen stellen muss.
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("OWNER")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @Inject
    JsonWebToken jwt;

    /**
     * Gibt alle aggregierten Dashboard-Kennzahlen zurück.
     *
     * @return HTTP 200 mit {@link DashboardStats}; alle Felder sind immer befüllt
     */
    @GET
    public Response getDashboard() {
        String handwerkerId = jwt.getSubject();

        DashboardStats stats = dashboardService.getDashboardStats(handwerkerId);
        return Response.ok(stats).build();
    }
}
