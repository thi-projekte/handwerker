package de.winfprojekt.craftvoice.aiservice.model;

/**
 * Welche Pipeline-Variante der ai-service ausfuehrt.
 *
 * <p>Die Entscheidung wird zur Laufzeit aus den Feldern der {@link ProcessRequest}
 * abgeleitet — das BPMN gibt seit der Umstellung KEIN explizites {@code typ}-Feld
 * mehr mit (siehe {@link de.winfprojekt.craftvoice.aiservice.pipeline.ProcessTypeDetector}).
 *
 * <p><b>Offene Frage (Stand 2026-05-24):</b> Mit dem BPMN-Team klaeren, ob ein
 * explizites {@code typ}-Feld im Connector-Payload sinnvoll waere. Das wuerde die
 * Routing-Logik robuster machen (kein Raten anhand Feld-Anwesenheit) und der
 * Schnittstelle einen klaren Vertrag geben.
 */
public enum ProcessType {

    /**
     * Erstmaliger KI-Aufruf: aus {@code sprachschnipsel} + {@code vorlage} wird ein
     * neuer Angebotsentwurf erzeugt.
     */
    ERSTANGEBOT,

    /**
     * Korrektur eines bestehenden Entwurfs: {@code angebotsentwurf} +
     * {@code korrekturschnipsel} werden zu einer angepassten Position-Liste.
     */
    KORREKTUR
}
