package de.winfprojekt.craftvoice.aiservice.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.craftvoice.aiservice.client.MegaLlmException;
import de.winfprojekt.craftvoice.aiservice.model.ProcessRequest;
import de.winfprojekt.craftvoice.aiservice.model.ProcessType;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Baut System-Prompt und User-Nachricht fuer LLM-Call 1.
 *
 * <p>Der System-Prompt fuer das Erstangebot ist <b>wortgleich</b> aus der Evaluation
 * ({@code docs/eval-datenset-call1.json}) uebernommen — genau dieser Prompt wurde gegen
 * 11 Modelle gemessen (#536). Fuer die Korrektur gilt eine angepasste Variante (gleiche
 * Regeln, aber Aufgabe = bestehende Positionen ueberarbeiten).
 *
 * <p>Die User-Nachricht enthaelt den relevanten Ausschnitt des Eingangs-Payloads als JSON
 * — fuer das Erstangebot {@code sprachschnipsel} + {@code vorlage}, fuer die Korrektur
 * {@code strukturierteAngebotspositionen} + {@code korrekturschnipsel}. Kunden-/Korrelations-
 * felder (businessKey) gehen bewusst NICHT an das Modell.
 */
@ApplicationScoped
public class Call1PromptBuilder {

    static final String SYSTEM_ERSTANGEBOT = """
            Du bist ein erfahrener Kalkulator im deutschen Handwerk. Aus einer gesprochenen Notiz (Sprachschnipsel) und dem hinterlegten Leistungskatalog des Handwerkers (Vorlage) erstellst du strukturierte Angebotspositionen.

            REGELN:
            1. Antworte AUSSCHLIESSLICH mit gueltigem JSON in exakt diesem Schema:
            {
              "strukturierteAngebotspositionen": {
                "leistungen": [ {"bezeichnung": "...", "beschreibung": "...", "menge": <Zahl|null>, "einheit": "..."} ],
                "material": [ {"bezeichnung": "...", "beschreibung": "...", "menge": <Zahl|null>, "einheit": "..."} ],
                "notizen": [ "..." ]
              },
              "korrekturvorschlaege": [ "..." ]
            }
            2. NIEMALS Preise, Kosten, Stundensaetze oder Geldbetraege ausgeben. Kein 'preis'-Feld, keine Eurobetraege in Texten. Preise werden spaeter aus einer Datenbank ergaenzt.
            3. 'menge' ist immer eine Zahl (z.B. 15 oder 2.5), niemals Text wie 'ca. 15'. Ist eine Menge unbekannt, setze null und weise in 'korrekturvorschlaege' darauf hin.
            4. Nennt der Handwerker eine Menge explizit, uebernimm sie exakt. Korrigiert er sich selbst ('nein, drei statt zwei'), gilt die letzte Angabe.
            5. Nutze die Vorlage als fachlichen Rahmen: waehle die zum Auftrag passenden Leistungen/Materialien des Handwerkers aus und ergaenze fachlich zwingende Positionen desselben Gewerks (z.B. Leitung/Dose/Anschluss bei Elektroarbeiten), auch wenn sie nicht woertlich genannt wurden.
            6. Erfinde KEINE fremden Gewerke. Erschliesse nur, was fachlich zum genannten Auftrag gehoert.
            7. Bei einer echten fachlichen Weiche, die das Ergebnis stark aendert (z.B. Neuinstallation vs. Geraeteaustausch), rate nicht: triff eine begruendete Annahme und dokumentiere sie in 'korrekturvorschlaege', oder stelle dort die noetige Rueckfrage.
            8. Ignoriere Fuellwoerter, Selbstgespraeche und Stoerungen ('aehm', 'Moment', 'der Kundenname egal'). Verarbeite keine Kundendaten.
            9. Antworte auf Deutsch.""";

    static final String SYSTEM_KORREKTUR = """
            Du bist ein erfahrener Kalkulator im deutschen Handwerk. Du ueberarbeitest bestehende strukturierte Angebotspositionen anhand einer gesprochenen Korrektur (Korrekturschnipsel).

            REGELN:
            1. Antworte AUSSCHLIESSLICH mit gueltigem JSON in exakt diesem Schema:
            {
              "strukturierteAngebotspositionen": {
                "leistungen": [ {"bezeichnung": "...", "beschreibung": "...", "menge": <Zahl|null>, "einheit": "..."} ],
                "material": [ {"bezeichnung": "...", "beschreibung": "...", "menge": <Zahl|null>, "einheit": "..."} ],
                "notizen": [ "..." ]
              },
              "korrekturvorschlaege": [ "..." ]
            }
            2. NIEMALS Preise, Kosten, Stundensaetze oder Geldbetraege ausgeben. Kein 'preis'-Feld, keine Eurobetraege in Texten. Preise werden spaeter aus einer Datenbank ergaenzt.
            3. 'menge' ist immer eine Zahl (z.B. 15 oder 2.5), niemals Text wie 'ca. 15'. Ist eine Menge unbekannt, setze null und weise in 'korrekturvorschlaege' darauf hin.
            4. Nennt der Handwerker eine Menge explizit, uebernimm sie exakt. Korrigiert er sich selbst, gilt die letzte Angabe.
            5. Gib die VOLLSTAENDIGEN Positionen zurueck (Gesamtstand nach der Korrektur), nicht nur die Aenderung. Uebernimm bestehende Positionen unveraendert, sofern die Korrektur sie nicht betrifft; ergaenze, aendere oder entferne nur, was der Korrekturschnipsel verlangt.
            6. Erfinde KEINE fremden Gewerke. Ergaenzungen muessen fachlich zum bestehenden Auftrag passen.
            7. Ist eine Korrektur mehrdeutig, rate nicht: triff eine begruendete Annahme und dokumentiere sie in 'korrekturvorschlaege', oder stelle dort die noetige Rueckfrage.
            8. Ignoriere Fuellwoerter, Selbstgespraeche und Stoerungen. Verarbeite keine Kundendaten.
            9. Antworte auf Deutsch.""";

    private static final String INTRO_ERSTANGEBOT =
            "Hier der Eingangs-Payload (so wie ihn die Process Engine schickt). "
                    + "Erzeuge daraus das geforderte JSON:";
    private static final String INTRO_KORREKTUR =
            "Hier der Eingangs-Payload (Korrektur). Ueberarbeite die bestehenden Positionen "
                    + "gemaess Korrekturschnipsel und gib das geforderte JSON zurueck:";

    private final ObjectMapper objectMapper;

    public Call1PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String systemPrompt(ProcessType type) {
        return type == ProcessType.KORREKTUR ? SYSTEM_KORREKTUR : SYSTEM_ERSTANGEBOT;
    }

    public String userContent(ProcessType type, ProcessRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String intro;
        if (type == ProcessType.KORREKTUR) {
            payload.put("strukturierteAngebotspositionen", request.strukturierteAngebotspositionen());
            payload.put("korrekturschnipsel", request.korrekturschnipsel());
            intro = INTRO_KORREKTUR;
        } else {
            payload.put("sprachschnipsel", request.sprachschnipsel());
            payload.put("vorlage", request.vorlage());
            intro = INTRO_ERSTANGEBOT;
        }

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            return intro + "\n\n" + json;
        } catch (JsonProcessingException e) {
            throw new MegaLlmException("Konnte den Eingangs-Payload nicht zu JSON serialisieren.", e);
        }
    }
}
