package de.winfprojekt.craftvoice.aiservice.pipeline;

import de.winfprojekt.craftvoice.aiservice.client.CatalogCandidate;
import de.winfprojekt.craftvoice.aiservice.model.Angebotspositionen;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.Position;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM-Call 2 (#541): waehlt fuer jede <b>Materialposition</b> ein konkretes Katalogprodukt
 * und haengt dessen Katalog-ID an die Position ({@link Position#withKatalogProduktId(Long)}).
 *
 * <p>Pro Position: Kandidaten ueber {@link CatalogSearchService} holen (Vorfilter, ohne
 * Preise) → LLM waehlt genau einen oder {@code KEIN_TREFFER}. Leistungspositionen bleiben
 * unberuehrt (sie haben kein Katalogprodukt).
 *
 * <p><b>Parallelisierung:</b> Die Positionen werden gleichzeitig verarbeitet
 * ({@link ManagedExecutor}). Das ist entscheidend, weil Call 2 pro Position einmal laeuft —
 * bei grossen Angeboten (40–60 Positionen) waere sequenzielles Vorgehen inakzeptabel langsam.
 *
 * <p><b>Baseline-Fallback (Hybrid-Gedanke):</b> Ohne {@code MEGALLM_API_KEY} oder bei einem
 * LLM-Fehler wird der <b>Top-Kandidat der Suche</b> uebernommen (programmatische Baseline —
 * laut Eval ~Top-1 brauchbar). Das haelt den lokalen/Demo-Betrieb funktionsfaehig.
 * <b>Limitierung:</b> Im Baseline-Modus geht die „kein Treffer"-Faehigkeit verloren (es wird
 * immer der beste Kandidat genommen); die korrekte Ablehnung kann nur der echte LLM-Pfad.
 */
@ApplicationScoped
public class Call2Selector {

    private static final Logger LOG = Logger.getLogger(Call2Selector.class);
    private static final int CANDIDATE_LIMIT = 15;

    private final MegaLlmService megaLlm;
    private final CatalogSearchService catalogSearch;
    private final Call2PromptBuilder promptBuilder;
    private final ManagedExecutor executor;
    private final String model;

    public Call2Selector(MegaLlmService megaLlm,
                         CatalogSearchService catalogSearch,
                         Call2PromptBuilder promptBuilder,
                         ManagedExecutor executor,
                         @ConfigProperty(name = "megallm.model.default",
                                 defaultValue = "gemini-3-flash-preview") String model) {
        this.megaLlm = megaLlm;
        this.catalogSearch = catalogSearch;
        this.promptBuilder = promptBuilder;
        this.executor = executor;
        this.model = model;
    }

    /**
     * Reichert die Materialpositionen eines {@link ErgebnisKi} um Katalog-IDs an (parallel).
     * Leistungen, Notizen und Korrekturvorschlaege bleiben unveraendert.
     */
    public ErgebnisKi enrich(ErgebnisKi ergebnis) {
        if (ergebnis == null || ergebnis.strukturierteAngebotspositionen() == null) {
            return ergebnis;
        }
        Angebotspositionen ap = ergebnis.strukturierteAngebotspositionen();
        List<Position> material = ap.material();
        if (material == null || material.isEmpty()) {
            return ergebnis;
        }

        List<CompletableFuture<Position>> futures = new ArrayList<>(material.size());
        for (Position p : material) {
            futures.add(executor.supplyAsync(() -> selectFor(p)));
        }
        List<Position> enriched = new ArrayList<>(material.size());
        for (CompletableFuture<Position> f : futures) {
            enriched.add(f.join());
        }

        Angebotspositionen neu = new Angebotspositionen(ap.leistungen(), enriched, ap.notizen());
        return new ErgebnisKi(neu, ergebnis.korrekturvorschlaege(),
                ergebnis.geschaetzteArbeitsdauerStunden());
    }

    /** Waehlt fuer eine einzelne Materialposition das Katalogprodukt. */
    Position selectFor(Position position) {
        String query = ((position.bezeichnung() == null ? "" : position.bezeichnung()) + " "
                + (position.beschreibung() == null ? "" : position.beschreibung())).trim();

        List<CatalogCandidate> candidates = catalogSearch.search(query, CANDIDATE_LIMIT);
        if (candidates.isEmpty()) {
            LOG.debugf("Call 2: keine Kandidaten fuer '%s' -> kein Katalogprodukt.", position.bezeichnung());
            return position;
        }

        if (!megaLlm.isConfigured()) {
            CatalogCandidate top = candidates.get(0);
            LOG.warnf("Call 2: kein MEGALLM_API_KEY -> Baseline (Top-Kandidat) fuer '%s': %s.",
                    position.bezeichnung(), top.articleNumber());
            return position.withKatalogProduktId(top.id());
        }

        try {
            String raw = megaLlm.complete(model, promptBuilder.system(),
                    promptBuilder.userContent(position, candidates));
            String pick = promptBuilder.parsePick(raw);

            if (Call2PromptBuilder.KEIN_TREFFER.equals(pick)) {
                LOG.debugf("Call 2: LLM -> KEIN_TREFFER fuer '%s'.", position.bezeichnung());
                return position;
            }

            CatalogCandidate chosen = candidates.stream()
                    .filter(c -> pick.equalsIgnoreCase(c.articleNumber()))
                    .findFirst()
                    .orElse(null);
            if (chosen == null) {
                LOG.warnf("Call 2: LLM-Pick '%s' ist nicht in der Kandidatenliste fuer '%s' -> kein Produkt.",
                        pick, position.bezeichnung());
                return position;
            }

            LOG.infof("Call 2: '%s' -> %s (%s).",
                    position.bezeichnung(), chosen.articleNumber(), chosen.name());
            return position.withKatalogProduktId(chosen.id());

        } catch (RuntimeException e) {
            CatalogCandidate top = candidates.get(0);
            LOG.errorf(e, "Call 2 fehlgeschlagen fuer '%s' -> Baseline Top-Kandidat %s.",
                    position.bezeichnung(), top.articleNumber());
            return position.withKatalogProduktId(top.id());
        }
    }
}
