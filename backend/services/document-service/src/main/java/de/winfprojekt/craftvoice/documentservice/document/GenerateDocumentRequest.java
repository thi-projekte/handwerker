package de.winfprojekt.craftvoice.documentservice.document;

import com.fasterxml.jackson.databind.JsonNode;

public class GenerateDocumentRequest {

    public String businessKey;

    public String typ;

    public String format;

    public String customerId;

    public String dokumentart;

    public JsonNode angebotsentwurf;

    public JsonNode rechnungsentwurf;
}