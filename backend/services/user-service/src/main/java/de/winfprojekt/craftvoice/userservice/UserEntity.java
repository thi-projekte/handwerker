package de.winfprojekt.craftvoice.userservice;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @NotBlank
    @Email
    @Column(unique = true)
    public String email;

    @Column(unique = true)
    public String keycloakId;

    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    public UserStatus status = UserStatus.PENDING;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    public Set<UserRole> roles = new HashSet<>();

    // Company Metadata
    public String companyName;
    public String vatId;
    public String tradeRegisterNumber;
    public String companyAddress;

    // AI Style Preferences
    public String toneOfVoice; // e.g. "Du" vs "Sie"
    public String detailLevel; // e.g. "kurz & prägnant" vs "detailliert"

    // Text Blocks
    @Column(columnDefinition = "TEXT")
    public String agbNotes;
    @Column(columnDefinition = "TEXT")
    public String paymentTerms;

    public static UserEntity findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public static UserEntity findByKeycloakId(String keycloakId) {
        return find("keycloakId", keycloakId).firstResult();
    }
}

enum UserStatus {
    PENDING, ACTIVE, DELETED
}

enum UserRole {
    OWNER, EMPLOYEE, ACCOUNTANT, CUSTOMER
}
