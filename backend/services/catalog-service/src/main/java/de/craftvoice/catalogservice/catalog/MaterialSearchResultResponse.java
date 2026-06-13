package de.craftvoice.catalogservice.catalog;

import java.util.List;

public class MaterialSearchResultResponse {

    public List<MaterialSearchResponse> candidates;

    public MaterialSearchResultResponse(List<MaterialSearchResponse> candidates) {
        this.candidates = candidates;
    }
}