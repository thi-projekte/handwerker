package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.CatalogCandidate;
import de.winfprojekt.craftvoice.aiservice.model.Position;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Baut System-Prompt und User-Nachricht fuer LLM-Call 2 (Produktauswahl) und parst die
 * Antwort. Prompt und Parse-Logik sind <b>wortgleich</b> aus der Evaluation
 * ({@code eval/call2-llm-eval.mjs}) uebernommen — damit das Produktionsverhalten dem
 * entspricht, was gemessen wurde (alle Modelle 16/16).
 *
 * <p>Die Kandidaten werden <b>neutral nach articleNumber sortiert</b> praesentiert, damit das
 * Modell nicht einfach "Rang 1" abschreibt. Es muss genau eine articleNumber waehlen oder
 * {@code KEIN_TREFFER} zurueckgeben. Preise sind bewusst nicht enthalten.
 */
@ApplicationScoped
public class Call2PromptBuilder {

    public static final String KEIN_TREFFER = "KEIN_TREFFER";

    static final String SYSTEM =
            "Du bist ein erfahrener Kalkulator im deutschen Handwerk. Zu einer Angebotsposition bekommst du "
            + "eine Liste von Produktkandidaten aus dem Katalog. Waehle GENAU EINEN Kandidaten, der fachlich am "
            + "besten zur Position passt.\n"
            + "WICHTIG: Wenn KEIN Kandidat wirklich passt (falsche Art, falsches Material, oder das Gesuchte ist "
            + "gar nicht dabei), dann waehle NICHT erzwungen, sondern gib articleNumber = \"KEIN_TREFFER\".\n"
            + "Die articleNumber MUSS exakt aus der Kandidatenliste stammen (oder \"KEIN_TREFFER\"). Preise sind "
            + "nicht angegeben und spielen keine Rolle.\n"
            + "Antworte AUSSCHLIESSLICH mit JSON: {\"articleNumber\": \"<...>\", \"begruendung\": \"<kurz, 1 Satz>\"}";

    private static final Pattern FENCE_START = Pattern.compile("(?s)^\\s*```(?:json)?\\s*");
    private static final Pattern FENCE_END = Pattern.compile("(?s)\\s*```\\s*$");
    // Artikelnummern des echten catalog-service sind LETTERS-SEGMENT(-SEGMENT)*, z.B.
    // SCH-JUNG-SD, KAB-NYM315, INS-KAI-GD2-25 (nicht nur das Mock-Format ELE-3004). Das
    // alte Muster "[A-Z]{2,5}-\\d+" matchte NUR Mock-Nummern -> bei echten Nummern im
    // JSON-Fallback/der Normalisierung wurde nichts erkannt.
    private static final Pattern ARTICLE = Pattern.compile("[A-Z]{2,6}(?:-[A-Z0-9]+)+");
    private static final Pattern KEIN_TREFFER_TEXT =
            Pattern.compile("^(kein[_\\s-]?treffer|none|null|keiner?)$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public Call2PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String system() {
        return SYSTEM;
    }

    public String userContent(Position position, List<CatalogCandidate> kandidaten) {
        List<CatalogCandidate> neutral = new ArrayList<>(kandidaten);
        neutral.sort(Comparator.comparing(c -> c.articleNumber() == null ? "" : c.articleNumber()));

        StringBuilder sb = new StringBuilder();
        sb.append("POSITION:\n")
                .append("  Bezeichnung: ").append(nz(position.bezeichnung())).append('\n')
                .append("  Beschreibung: ").append(position.beschreibung() == null || position.beschreibung().isBlank()
                        ? "(keine)" : position.beschreibung()).append('\n')
                .append("  Menge: ").append(position.menge() == null ? "?" : position.menge())
                .append(' ').append(nz(position.einheit())).append("\n\n")
                .append("KANDIDATEN (").append(neutral.size()).append("):\n");
        for (CatalogCandidate k : neutral) {
            sb.append("- ").append(nz(k.articleNumber())).append(" | ").append(nz(k.name()))
                    .append(" | ").append(nz(k.description())).append(" | Einheit: ").append(nz(k.unit()))
                    .append('\n');
        }
        sb.append("\nWaehle den passenden articleNumber oder \"KEIN_TREFFER\". Nur JSON.");
        return sb.toString();
    }

    /**
     * Extrahiert die gewaehlte articleNumber aus der Modell-Antwort — oder {@link #KEIN_TREFFER}.
     * Toleriert Markdown-Fences, fehlendes JSON (Regex-Fallback) und diverse "kein Treffer"-
     * Schreibweisen. Die Gueltigkeit gegen die Kandidatenliste prueft der Aufrufer.
     */
    public String parsePick(String rawOutput) {
        String t = rawOutput == null ? "" : rawOutput.trim();
        t = FENCE_START.matcher(t).replaceAll("");
        t = FENCE_END.matcher(t).replaceAll("").trim();

        String art = null;
        try {
            JsonNode o = objectMapper.readTree(t);
            if (o != null && o.isObject()) {
                JsonNode an = o.has("articleNumber") ? o.get("articleNumber") : o.get("article_number");
                if (an != null && !an.isNull()) {
                    art = an.asText();
                }
            }
        } catch (Exception ignore) {
            Matcher m = ARTICLE.matcher(t);
            if (m.find()) {
                art = m.group();
            } else if (Pattern.compile("kein[_\\s-]?treffer", Pattern.CASE_INSENSITIVE).matcher(t).find()) {
                art = KEIN_TREFFER;
            }
        }

        if (art == null || KEIN_TREFFER_TEXT.matcher(art.trim()).matches()) {
            return KEIN_TREFFER;
        }
        Matcher norm = ARTICLE.matcher(art.trim().toUpperCase());
        return norm.find() ? norm.group() : art.trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
