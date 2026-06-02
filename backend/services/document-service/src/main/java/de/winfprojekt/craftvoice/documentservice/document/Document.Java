package de.winfprojekt.craftvoice.documentservice.document;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public DocumentType type;

    @Column(nullable = false)
    public UUID offerId;

    public UUID customerId;

    @Column(nullable = false)
    public String fileName;

    @Column(nullable = false, length = 1000)
    public String filePath;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant updatedAt;
}