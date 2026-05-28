package de.winfprojekt.craftvoice.offerservice.common;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {

    @Column(name = "erstellt_am", updatable = false)
    public LocalDateTime erstelltAm;

    @Column(name = "aktualisiert_am")
    public LocalDateTime aktualisiertAm;

    @PrePersist
    void beiPersistierung() {
        erstelltAm = LocalDateTime.now();
        aktualisiertAm = LocalDateTime.now();
    }

    @PreUpdate
    void beiAktualisierung() {
        aktualisiertAm = LocalDateTime.now();
    }
}