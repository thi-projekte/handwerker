package de.winfprojekt.craftvoice.documentservice.document;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public DocumentType type;

    @Column(nullable = false)
    public String referenceId;

    @Column(nullable = false)
    public String fileName;

    @Column(nullable = false)
    public String contentType = "application/pdf";

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    public byte[] pdfContent;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}