package de.winfprojekt.craftvoice.offerservice.speechcapture;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeepgramResponse(Results results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(List<Channel> channels) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(List<Alternative> alternatives) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alternative(String transcript) {}
}
