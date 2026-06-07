package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.MegaLlmException;
import de.winfprojekt.craftvoice.aiservice.model.Angebotspositionen;
import de.winfprojekt.craftvoice.aiservice.model.ErgebnisKi;
import de.winfprojekt.craftvoice.aiservice.model.Position;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Parst die rohe Modell-Ausgabe robust zu einem {@link ErgebnisKi}.
 *
 * <p>Toleriert (uebernommen aus {@code eval/lib/megallm.mjs}, dort als noetig erkannt):
 * <ul>
 *   <li>Markdown-Code-Fences ({@code ```json ... ```}) um das JSON,</li>
 *   <li>einen fehlenden Wrapper (manche Modelle geben {@code {leistungen, material, notizen}}
 *       direkt ohne {@code strukturierteAngebotspositionen} zurueck),</li>
 *   <li>fehlende Teil-Arrays (werden zu leeren Listen).</li>
 * </ul>
 *
 * <p>Ist die Ausgabe gar kein JSON-Objekt, wird eine {@link MegaLlmException} geworfen —
 * der {@link LlmCall1Generator} weicht dann auf den Stub aus.
 */
@ApplicationScoped
public class ErgebnisKiParser {

    private final ObjectMapper objectMapper;

    public ErgebnisKiParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ErgebnisKi parse(String rawOutput) {
        String text = rawOutput == null ? "" : rawOutput.strip();

        // 1) Markdown-Fences entfernen
        if (text.startsWith("```")) {
            text = text.replaceFirst("(?s)^```(?:json)?\\s*", "")
                    .replaceFirst("(?s)\\s*```\\s*$", "")
                    .strip();
        }

        // 2) JSON parsen
        JsonNode root;
        try {
            root = objectMapper.readTree(text);
        } catch (JsonProcessingException e) {
            throw new MegaLlmException("LLM-Antwort ist kein gueltiges JSON.", e);
        }
        if (root == null || !root.isObject()) {
            throw new MegaLlmException("LLM-Antwort ist kein JSON-Objekt.");
        }

        // 3) Wrapper normalisieren (fehlt er, ist root selbst der innere Block)
        JsonNode sap = root.get("strukturierteAngebotspositionen");
        if (sap == null && (root.has("leistungen") || root.has("material") || root.has("notizen"))) {
            sap = root;
        }

        List<Position> leistungen = readPositions(sap, "leistungen");
        List<Position> material = readPositions(sap, "material");
        List<String> notizen = readStrings(sap, "notizen");
        List<String> korrekturvorschlaege = readStrings(root, "korrekturvorschlaege");
        // Nur gesetzt, wenn der Handwerker eine Dauer ausgesprochen hat (#538-Folge); sonst null.
        Double geschaetzteArbeitsdauerStunden = readNumber(root, "geschaetzteArbeitsdauerStunden");

        return new ErgebnisKi(
                new Angebotspositionen(leistungen, material, notizen),
                korrekturvorschlaege,
                geschaetzteArbeitsdauerStunden);
    }

    private List<Position> readPositions(JsonNode parent, String field) {
        List<Position> out = new ArrayList<>();
        JsonNode arr = parent == null ? null : parent.get(field);
        if (arr == null || !arr.isArray()) {
            return out;
        }
        for (JsonNode n : arr) {
            out.add(new Position(
                    readText(n, "bezeichnung"),
                    readText(n, "beschreibung"),
                    readNumber(n, "menge"),
                    readText(n, "einheit")));
        }
        return out;
    }

    private List<String> readStrings(JsonNode parent, String field) {
        List<String> out = new ArrayList<>();
        JsonNode arr = parent == null ? null : parent.get(field);
        if (arr == null || !arr.isArray()) {
            return out;
        }
        for (JsonNode n : arr) {
            if (n != null && !n.isNull()) {
                out.add(n.asText());
            }
        }
        return out;
    }

    private static String readText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Double readNumber(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() || !v.isNumber() ? null : v.asDouble();
    }
}
