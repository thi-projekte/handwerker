package de.winfprojekt.craftvoice.aiservice.client;

/**
 * Typed-Variable-Wrapper im Camunda REST API Format.
 *
 * <p>Camunda erwartet bei {@code processVariables}, dass jede Variable mit ihrem Typ
 * annotiert wird:
 * <pre>
 * {
 *   "value": "&lt;stringified-JSON&gt;",
 *   "type":  "Json"
 * }
 * </pre>
 *
 * <p>Fuer unsere ergebnisKI-Korrelation verwenden wir immer {@code type="Json"}, weil
 * die BPMN-Skripte das Ergebnis per {@code S(...)}-Spin-Funktion zu JSON konvertieren
 * (siehe {@code Sprachschnipselverarbeitung.bpmn} Receive Task).
 *
 * @param value JSON-String (vom Aufrufer serialisiert)
 * @param type  Camunda-Typ, fuer JSON immer {@code "Json"} (Gross-J!)
 */
public record JsonVariable(String value, String type) {

    public static JsonVariable ofJson(String jsonString) {
        return new JsonVariable(jsonString, "Json");
    }
}
