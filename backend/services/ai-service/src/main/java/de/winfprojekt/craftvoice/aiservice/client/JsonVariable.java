package de.winfprojekt.craftvoice.aiservice.client;

/**
 * Typed-Variable-Wrapper im Camunda REST API Format.
 *
 * <p>Camunda erwartet bei {@code processVariables}, dass jede Variable mit ihrem Typ
 * annotiert wird:
 * <pre>
 * {
 *   "value": "&lt;stringified-JSON&gt;",
 *   "type":  "String"
 * }
 * </pre>
 *
 * <p><b>Warum {@code type="String"} und nicht {@code "Json"}?</b> Der
 * Schnittstellenvertrag mit der Process Engine (Stand 29.05.2026) sendet
 * {@code ergebnisKI} als String-Variable. Der ExecutionListener des Receive Task in
 * {@code Sprachschnipselverarbeitung.bpmn} liest die Variable ohnehin per
 * {@code .toString()} aus und konvertiert sie selbst per {@code S(...)}-Spin-Funktion
 * zu JSON. Ein Camunda-Typ {@code "Json"} wuerde nur dazu fuehren, dass Camunda den
 * Inhalt erst parst und der Listener ihn wieder serialisiert und neu parst —
 * unnoetig. {@code "String"} ist also der natuerliche und vertragskonforme Typ.
 *
 * @param value JSON-String (vom Aufrufer serialisiert)
 * @param type  Camunda-Typ, hier {@code "String"} (die PE parst den Inhalt selbst)
 */
public record JsonVariable(String value, String type) {

    public static JsonVariable ofJson(String jsonString) {
        return new JsonVariable(jsonString, "String");
    }
}
