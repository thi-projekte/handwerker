package de.winfprojekt.craftvoice.documentservice.exception;

import jakarta.ws.rs.NotFoundException;

public class DocumentNotFoundException extends NotFoundException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}