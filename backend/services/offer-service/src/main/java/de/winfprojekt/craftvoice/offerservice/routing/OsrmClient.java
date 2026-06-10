package de.winfprojekt.craftvoice.offerservice.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Kapselt Geocodierung (Nominatim) und Routing (OSRM) zur Distanzermittlung
 * zwischen zwei Adressen.
 *
 * <p>Verwendet ausschließlich kostenlose, schlüssellose APIs:
 * <ul>
 *   <li>Nominatim: {@code https://nominatim.openstreetmap.org/search}</li>
 *   <li>OSRM: {@code http://router.project-osrm.org/route/v1/driving}</li>
 * </ul>
 *
 * <p>Fehler (Netzwerk, Parse, leere Ergebnisse) werden als {@link RoutingException}
 * geworfen. Der Aufrufer entscheidet, ob er abbricht oder die Position überspringt.
 */
@ApplicationScoped
public class OsrmClient {

    private static final Logger LOG = Logger.getLogger(OsrmClient.class);

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?q={query}&format=json&limit=1";
    private static final String OSRM_URL =
            "http://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=false";

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Ermittelt die Fahrdistanz in km zwischen zwei Adressen via Nominatim + OSRM.
     *
     * @param adresseVon Startadresse (z. B. Handwerkeradresse)
     * @param adresseBis Zieladresse (z. B. Kundenadresse)
     * @return Distanz in km, gerundet auf 2 Dezimalstellen
     * @throws RoutingException bei Netzwerkfehler, unbekannter Adresse oder Parse-Fehler
     */
    public BigDecimal getDistanzKm(String adresseVon, String adresseBis) throws RoutingException {
        double[] koordinatenVon = geocodiere(adresseVon);
        double[] koordinatenBis = geocodiere(adresseBis);

        double lon1 = koordinatenVon[1];
        double lat1 = koordinatenVon[0];
        double lon2 = koordinatenBis[1];
        double lat2 = koordinatenBis[0];

        String osrmUrl = OSRM_URL
                .replace("{lon1}", String.valueOf(lon1))
                .replace("{lat1}", String.valueOf(lat1))
                .replace("{lon2}", String.valueOf(lon2))
                .replace("{lat2}", String.valueOf(lat2));

        LOG.debugf("OSRM Anfrage: %s", osrmUrl);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(osrmUrl))
                    .header("User-Agent", "craftvoice-offer-service/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RoutingException("OSRM antwortete mit HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode routes = root.path("routes");

            if (routes.isEmpty() || !routes.isArray()) {
                throw new RoutingException("OSRM lieferte keine Route für die gegebenen Koordinaten.");
            }

            double distanzMeter = routes.get(0).path("distance").asDouble();
            BigDecimal distanzKm = BigDecimal.valueOf(distanzMeter / 1000.0)
                    .setScale(2, RoundingMode.HALF_UP);

            LOG.debugf("Distanz ermittelt: %.2f km", distanzKm);
            return distanzKm;

        } catch (RoutingException e) {
            throw e;
        } catch (Exception e) {
            throw new RoutingException("Fehler bei OSRM-Routing: " + e.getMessage(), e);
        }
    }

    /**
     * Geocodiert eine Adresse via Nominatim und gibt [lat, lon] zurück.
     *
     * @param adresse Die zu geocodierende Adresse
     * @return double-Array mit [latitude, longitude]
     * @throws RoutingException wenn die Adresse nicht gefunden wurde oder ein Fehler auftrat
     */
    private double[] geocodiere(String adresse) throws RoutingException {
        String encodedAdresse = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
        String url = NOMINATIM_URL.replace("{query}", encodedAdresse);

        LOG.debugf("Nominatim Anfrage für Adresse: %s", adresse);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "craftvoice-offer-service/1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RoutingException("Nominatim antwortete mit HTTP " + response.statusCode()
                        + " für Adresse: " + adresse);
            }

            JsonNode results = objectMapper.readTree(response.body());

            if (results.isEmpty() || !results.isArray()) {
                throw new RoutingException("Nominatim fand keine Koordinaten für Adresse: " + adresse);
            }

            JsonNode erstes = results.get(0);
            double lat = erstes.path("lat").asDouble();
            double lon = erstes.path("lon").asDouble();

            LOG.debugf("Geocodierung erfolgreich: %s → lat=%.6f, lon=%.6f", adresse, lat, lon);
            return new double[]{lat, lon};

        } catch (RoutingException e) {
            throw e;
        } catch (Exception e) {
            throw new RoutingException("Fehler bei Nominatim-Geocodierung für '" + adresse + "': " + e.getMessage(), e);
        }
    }
}
