package de.winfprojekt.craftvoice.documentservice.document;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        DocumentType type,
        String referenceId,
        String fileName,
        String contentType,
        LocalDateTime createdAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.id,
                document.type,
                document.referenceId,
                document.fileName,
                document.contentType,
                document.createdAt
        );
    }
}