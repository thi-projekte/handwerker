package de.winfprojekt.craftvoice.offerservice.processengine.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Standardisiertes Nachrichten-Payload für die Process Engine.
 */
public class PeMessagePayload {

    public String messageName;
    public String businessKey;
    public Map<String, Object> processVariables = new HashMap<>();
    public boolean resultEnabled;

    public PeMessagePayload(
            String messageName,
            String businessKey,
            Map<String, Object> processVariables,
            boolean resultEnabled) {

        this.messageName = messageName;
        this.businessKey = businessKey;
        this.processVariables = processVariables != null
                ? processVariables
                : new HashMap<>();
        this.resultEnabled = resultEnabled;
    }
}