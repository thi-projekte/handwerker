package de.winfprojekt.craftvoice.userservice;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
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

    // Detailed Address
    public String street;
    public String houseNumber;
    public String zipCode;
    public String city;
    public String state;
    public String country;

    // Company Contact
    public String companyEmail;
    public String companyPhoneNumber;
    public String website;
    public String industry;

    // Banking Info
    public String iban;
    public String bic;
    public String bankName;
    public String accountHolder;

    // Tax Info
    public String taxNumber;
    public String legalForm;

    // Business Details
    public Integer employeeCount;
    public Integer customerCount;
    public Double hourlyRate;
    public String priceListUrl;

    // Travel / Anfahrtskosten configuration
    // Allowed values: "PAUSCHALE", "PAUSCHALE_PLUS_KM", "NUR_KM"
    public String travelModel;

    @Column(precision = 19, scale = 4)
    public BigDecimal travelFlatRate;

    @Column(precision = 19, scale = 4)
    public BigDecimal travelKmRate;

    // AI Style Preferences
    public String toneOfVoice;
    public String detailLevel;

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
    PENDING,
    ACTIVE,
    DELETED
}

enum UserRole {
    OWNER,
    EMPLOYEE,
    ACCOUNTANT,
    CUSTOMER
}