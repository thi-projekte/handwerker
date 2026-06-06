package de.winfprojekt.craftvoice.offerservice.routing;

/**
 * Wird geworfen, wenn die Geocodierung (Nominatim) oder das Routing (OSRM)
 * fehlschlägt. Der Aufrufer fängt diese Exception und überspringt die
 * Anfahrtsposition still — kein HTTP 5xx wird propagiert.
 */
public class RoutingException extends Exception {

    public RoutingException(String message) {
        super(message);
    }

    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}
