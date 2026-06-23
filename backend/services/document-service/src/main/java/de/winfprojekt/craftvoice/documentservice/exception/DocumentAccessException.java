package de.winfprojekt.craftvoice.documentservice.exception;

import jakarta.ws.rs.ForbiddenException;

public class DocumentAccessException extends ForbiddenException {

    public DocumentAccessException(String message) {
        super(message);
    }
}