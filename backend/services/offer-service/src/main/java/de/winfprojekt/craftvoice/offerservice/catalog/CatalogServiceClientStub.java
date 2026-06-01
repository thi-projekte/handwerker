// Platzhalter
// Kann gelöscht werden, sobald der eigentliche Catalog-Service steht.

package de.winfprojekt.craftvoice.offerservice.catalog;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

/**
 * Stub-Implementierung des Catalog Service Clients.
 * Gibt einen Dummy-Preis von BigDecimal.ZERO zurück.
 */
@ApplicationScoped
public class CatalogServiceClientStub implements CatalogServiceClient {

    @Override
    public CatalogPriceResponse getPreis(Long katalogProduktId) {
        CatalogPriceResponse response = new CatalogPriceResponse();
        response.preis = BigDecimal.ZERO;
        return response;
    }
}
