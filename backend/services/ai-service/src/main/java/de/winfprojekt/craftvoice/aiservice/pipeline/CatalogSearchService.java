package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.CatalogCandidate;
import de.winfprojekt.craftvoice.aiservice.client.CatalogSearchClient;
import de.winfprojekt.craftvoice.aiservice.client.CatalogSearchResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Liefert Katalog-Kandidaten fuer LLM-Call 2 — entweder vom echten catalog-service
 * (#540) oder aus einem internen Mock (#539), je nach {@code catalog.mock.enabled}.
 *
 * <p><b>Mock-Betrieb</b> (Default, solange Michis Such-Endpoint fehlt): liest den
 * gebuendelten Mini-Katalog {@code /mock/catalog-mock.json} und rankt ihn mit einer
 * schlanken Token-Overlap-Suche. Das ist NICHT BM25 — es muss nur plausible Kandidaten
 * liefern, damit Call 2 lokal/in Tests laeuft. Das eigentliche (gemessene) Ranking macht
 * spaeter der catalog-service per Postgres-Volltextsuche (Spec: {@code docs/catalog-search-spec.md}).
 *
 * <p><b>Real-Betrieb</b>: ruft {@link CatalogSearchClient}. Faellt der Aufruf aus, wird eine
 * leere Liste zurueckgegeben (→ Call 2 behandelt das als "kein Treffer") statt zu werfen.
 */
@ApplicationScoped
public class CatalogSearchService {

    private static final Logger LOG = Logger.getLogger(CatalogSearchService.class);
    private static final String MOCK_RESOURCE = "/mock/catalog-mock.json";

    private final boolean mockEnabled;
    private final CatalogSearchClient client;
    private final List<CatalogCandidate> mockCatalog;

    @Inject
    public CatalogSearchService(@RestClient CatalogSearchClient client,
                                ObjectMapper objectMapper,
                                @ConfigProperty(name = "catalog.mock.enabled", defaultValue = "true")
                                boolean mockEnabled) {
        this.client = client;
        this.mockEnabled = mockEnabled;
        this.mockCatalog = mockEnabled ? ladeMockKatalog(objectMapper) : List.of();
        if (mockEnabled) {
            LOG.infof("CatalogSearchService im MOCK-Betrieb (%d Artikel) — catalog.mock.enabled=true.",
                    mockCatalog.size());
        }
    }

    /**
     * Liefert bis zu {@code limit} Kandidaten zur Freitext-Query, absteigend nach Relevanz.
     * Bei keinem Treffer (oder Fehler im Real-Betrieb) eine leere Liste.
     */
    public List<CatalogCandidate> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return mockEnabled ? sucheImMock(query, limit) : sucheRemote(query, limit);
    }

    // ---------- Real-Betrieb ----------

    private List<CatalogCandidate> sucheRemote(String query, int limit) {
        try {
            CatalogSearchResponse response = client.search(query, limit);
            List<CatalogCandidate> candidates = response != null ? response.candidates() : null;
            return candidates != null ? candidates : List.of();
        } catch (RuntimeException e) {
            LOG.warnf("catalog-service-Suche fehlgeschlagen, behandle als 'kein Treffer': %s",
                    e.getMessage());
            return List.of();
        }
    }

    // ---------- Mock-Betrieb ----------

    private List<CatalogCandidate> sucheImMock(String query, int limit) {
        Set<String> queryTokens = new HashSet<>(tokenize(query));
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        return mockCatalog.stream()
                .map(c -> scored(c, queryTokens))
                .filter(s -> s.score() != null && s.score() > 0)
                .sorted(Comparator.comparingDouble((CatalogCandidate c) -> c.score()).reversed())
                .limit(limit)
                .toList();
    }

    private static CatalogCandidate scored(CatalogCandidate c, Set<String> queryTokens) {
        Set<String> docTokens = new HashSet<>(
                tokenize((c.name() + " " + c.description() + " " + c.categoryName())));
        long overlap = queryTokens.stream().filter(docTokens::contains).count();
        return new CatalogCandidate(c.id(), c.articleNumber(), c.name(), c.description(),
                c.unit(), c.categoryName(), (double) overlap);
    }

    private static List<String> tokenize(String text) {
        String folded = (text == null ? "" : text).toLowerCase()
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        List<String> tokens = new ArrayList<>();
        for (String t : folded.replaceAll("[^a-z0-9]+", " ").split("\\s+")) {
            if (t.length() >= 2) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private List<CatalogCandidate> ladeMockKatalog(ObjectMapper objectMapper) {
        try (InputStream is = getClass().getResourceAsStream(MOCK_RESOURCE)) {
            if (is == null) {
                LOG.errorf("Mock-Katalog %s nicht gefunden — Mock-Suche liefert nichts.", MOCK_RESOURCE);
                return List.of();
            }
            JsonNode root = objectMapper.readTree(is);
            JsonNode materials = root.get("materials");
            if (materials == null || !materials.isArray()) {
                LOG.errorf("Mock-Katalog %s hat kein 'materials'-Array.", MOCK_RESOURCE);
                return List.of();
            }
            List<CatalogCandidate> list = new ArrayList<>();
            for (JsonNode m : materials) {
                list.add(new CatalogCandidate(
                        m.path("id").isNull() ? null : m.path("id").asLong(),
                        text(m, "articleNumber"),
                        text(m, "name"),
                        text(m, "description"),
                        text(m, "unit"),
                        text(m, "categoryName"),
                        null));
            }
            return List.copyOf(list);
        } catch (Exception e) {
            LOG.errorf(e, "Mock-Katalog %s konnte nicht geladen werden.", MOCK_RESOURCE);
            return List.of();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    /** Nur fuer Tests: Mock-Service mit explizitem Katalog, ohne Classpath/CDI. */
    static CatalogSearchService fuerTest(List<CatalogCandidate> katalog) {
        return new CatalogSearchService(katalog);
    }

    private CatalogSearchService(List<CatalogCandidate> katalog) {
        this.client = null;
        this.mockEnabled = true;
        this.mockCatalog = katalog != null ? List.copyOf(katalog) : List.of();
    }
}
