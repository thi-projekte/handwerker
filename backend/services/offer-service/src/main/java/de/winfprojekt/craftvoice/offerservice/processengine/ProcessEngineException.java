package de.winfprojekt.craftvoice.offerservice.processengine;

/**
 * Exception, die geworfen wird, wenn bei der Kommunikation mit der Process Engine ein Fehler auftritt.
 */
public class ProcessEngineException extends RuntimeException {

    public ProcessEngineException(String message) {
        super(message);
    }

    public ProcessEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}