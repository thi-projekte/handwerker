package de.winfprojekt.craftvoice.offerservice.catalog;

/**
 * Interface für den Catalog Service Client.
 */
public interface CatalogServiceClient {

    /**
     * Ruft den Preis für ein Katalog-Produkt ab.
     *
     * @param katalogProduktId ID des Produkts im Katalog
     * @return Preisantwort
     */
    CatalogPriceResponse getPreis(Long katalogProduktId);
}
