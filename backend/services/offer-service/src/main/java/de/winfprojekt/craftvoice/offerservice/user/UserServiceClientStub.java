// Platzhalter
// Kann gelöscht werden, sobald der echte User-Service die Endpunkte bereitstellt.

package de.winfprojekt.craftvoice.offerservice.user;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

/**
 * Stub-Implementierung des {@link UserServiceClient}.
 *
 * <p>Gibt Testwerte zurück, die für lokale Entwicklung und Tests ausreichend sind:
 * <ul>
 *   <li>Stundensatz: 65,00 €/h</li>
 *   <li>Anfahrtsmodell: NUR_KM mit 0,30 €/km</li>
 *   <li>Handwerkeradresse: Maximilianstraße 1, 80538 München</li>
 * </ul>
 *
 * <p>Wird durch den echten Quarkus REST Client ersetzt, sobald Abstimmungspunkt 2
 * (user-service Endpunkte) geklärt ist.
 */
@ApplicationScoped
public class UserServiceClientStub implements UserServiceClient {

    @Override
    public StundensatzResponse getStundensatz() {
        StundensatzResponse response = new StundensatzResponse();
        response.stundensatz = new BigDecimal("65.00");
        return response;
    }

    @Override
    public AnfahrtskostenKonfiguration getAnfahrtskostenKonfiguration() {
        AnfahrtskostenKonfiguration konfig = new AnfahrtskostenKonfiguration();
        konfig.modell = "NUR_KM";
        konfig.pauschale = null;
        konfig.kmSatz = new BigDecimal("0.30");
        konfig.adresse = "Maximilianstraße 1, 80538 München";
        return konfig;
    }

    @Override
    public CustomerDTO getCustomer(Long customerId) {
        CustomerDTO customer = new CustomerDTO();
        customer.id = customerId;
        customer.email = "customer" + customerId + "@example.com";
        customer.firstName = "Max";
        customer.lastName = "Mustermann";
        customer.street = "Marienplatz";
        customer.houseNumber = "1";
        customer.zipCode = "80331";
        customer.city = "München";
        return customer;
    }
}
