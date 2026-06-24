package de.winfprojekt.craftvoice.documentservice.document;

import java.time.LocalDateTime;

public class DocumentResponse {

    public Long id;
    public DocumentType type;
    public String referenceId;
    public String customerId;
    public String ownerId;
    public String fileName;
    public LocalDateTime createdAt;
    public String recipientEmail;
    public String recipientName;

    public static DocumentResponse from(Document document) {
        if (document == null) {
            return null;
        }

        DocumentResponse response = new DocumentResponse();
        response.id = document.id;
        response.type = document.type;
        response.referenceId = document.referenceId;
        response.customerId = document.customerId;
        response.ownerId = document.ownerId;
        response.fileName = document.fileName;
        response.createdAt = document.createdAt;
        response.recipientEmail = document.recipientEmail;
        response.recipientName = document.recipientName;

        return response;
    }
}