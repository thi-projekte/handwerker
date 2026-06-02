package de.winfprojekt.craftvoice.userservice;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity extends BaseEntity {

    public Long userId;
    public String action; // e.g., "PASSWORD_CHANGE", "COMPANY_DATA_UPDATE"
    public String description;

    public static void log(Long userId, String action, String description) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.userId = userId;
        entry.action = action;
        entry.description = description;
        entry.persist();
    }
}
