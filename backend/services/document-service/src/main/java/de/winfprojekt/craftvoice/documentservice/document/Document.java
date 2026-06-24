package de.winfprojekt.craftvoice.documentservice.document;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
public class Document extends PanacheEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public DocumentType type;

    @Column(nullable = false)
    public String referenceId;

    @Column(nullable = false)
    public String customerId;

    @Column(nullable = false)
    public String ownerId;

    @Column(nullable = false)
    public String fileName;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    public byte[] pdfContent;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}