package de.winfprojekt.craftvoice.userservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@ApplicationScoped
public class UserService {

    @Inject
    Keycloak keycloak;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "DOCUMENT_STORAGE_PATH", defaultValue = "/tmp")
    String storagePath;

    private static final String REALM = "handwerker-realm";

    @Transactional
    public String uploadProfilePicture(Long userId, FileUpload file) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        try {
            String fileName =
                    "profile_" + userId + "_" + System.currentTimeMillis() + getExtension(file.fileName());

            Path path = Paths.get(storagePath, "profiles", fileName);
            Files.createDirectories(path.getParent());
            Files.copy(file.uploadedFile(), path);

            String url = "/api/users/profile-picture/" + fileName;
            user.profilePictureUrl = url;

            AuditLogEntity.log(
                    user.id,
                    "PROFILE_PICTURE_UPLOAD",
                    "User uploaded a new profile picture: " + fileName
            );

            return url;
        } catch (IOException e) {
            throw new RuntimeException("Could not save profile picture", e);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
    }

    @Transactional
    public void register(UserEntity userData, String password) {
        if (UserEntity.findByEmail(userData.email) != null) {
            throw new BadRequestException("Email already registered");
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(userData.email);
        user.setEmail(userData.email);
        user.setFirstName(userData.firstName);
        user.setLastName(userData.lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(false);
        user.setCredentials(Collections.singletonList(cred));

        Response response = keycloak.realm(REALM).users().create(user);

        if (response.getStatus() != 201) {
            throw new BadRequestException(
                    "Could not create user in Keycloak: " + response.readEntity(String.class)
            );
        }

        String keycloakId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        userData.keycloakId = keycloakId;
        userData.status = UserStatus.PENDING;
        userData.roles.add(UserRole.OWNER);
        userData.persist();

        keycloak.realm(REALM).users().get(keycloakId).sendVerifyEmail();

        // Assign default realm role in Keycloak (ensure role exists in the realm)
        try {
            org.keycloak.representations.idm.RoleRepresentation ownerRole = keycloak.realm(REALM).roles().get("OWNER").toRepresentation();
            if (ownerRole != null) {
                keycloak.realm(REALM).users().get(keycloakId).roles().realmLevel().add(java.util.Collections.singletonList(ownerRole));
            }
        } catch (Exception e) {
            // Log but don't fail registration if role assignment fails
            AuditLogEntity.log(userData.id, "ROLE_ASSIGNMENT_FAILED", "Assigning OWNER role in Keycloak failed: " + e.getMessage());
        }
        
        AuditLogEntity.log(userData.id, "REGISTRATION", "User registered via Keycloak");
    }

    @Transactional
    public UserEntity syncUserWithDatabase() {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaim("email");

        if (email == null || email.isBlank()) {
            throw new BadRequestException("User hat kein email-Claim im JWT-Token (Keycloak-Konfiguration überprüfen)");
        }

        UserEntity user = UserEntity.findByKeycloakId(keycloakId);

        if (user == null) {
            user = new UserEntity();
            user.keycloakId = keycloakId;
            user.email = email;
            user.firstName = jwt.getClaim("given_name");
            user.lastName = jwt.getClaim("family_name");
            user.status = UserStatus.ACTIVE;
            user.roles.add(UserRole.OWNER);
            user.persist();
        } else {
            user.email = email;

            if (user.firstName == null || user.firstName.isBlank()) {
                user.firstName = jwt.getClaim("given_name");
            }

            if (user.lastName == null || user.lastName.isBlank()) {
                user.lastName = jwt.getClaim("family_name");
            }
        }

        return user;
    }

    @Transactional
    public UserEntity updateProfile(Long userId, UserEntity data) {
        UserEntity user = UserEntity.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (data.firstName != null) {
            user.firstName = data.firstName;
        }

        if (data.lastName != null) {
            user.lastName = data.lastName;
        }

        if (data.phoneNumber != null) {
            user.phoneNumber = data.phoneNumber;
        }

        if (data.profilePictureUrl != null) {
            user.profilePictureUrl = data.profilePictureUrl;
        }

        try {
            if (user.keycloakId != null && !user.keycloakId.isBlank()) {
                UserRepresentation kcUser =
                        keycloak.realm(REALM).users().get(user.keycloakId).toRepresentation();

                if (data.firstName != null) {
                    kcUser.setFirstName(data.firstName);
                }

                if (data.lastName != null) {
                    kcUser.setLastName(data.lastName);
                }

                keycloak.realm(REALM).users().get(user.keycloakId).update(kcUser);
            }
        } catch (Exception e) {
            System.err.println(
                    "Keycloak profile sync failed for user "
                            + user.id
                            + ": "
                            + e.getMessage()
            );
        }

        AuditLogEntity.log(
                user.id,
                "PROFILE_UPDATE",
                "User updated personal data"
        );

        return user;
    }

    @Transactional
    public void updateCompanyData(Long userId, UserEntity data) {
        UserEntity user = UserEntity.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (data.companyName != null) user.companyName = data.companyName;
        if (data.vatId != null) user.vatId = data.vatId;
        if (data.tradeRegisterNumber != null) user.tradeRegisterNumber = data.tradeRegisterNumber;

        if (data.street != null) user.street = data.street;
        if (data.houseNumber != null) user.houseNumber = data.houseNumber;
        if (data.zipCode != null) user.zipCode = data.zipCode;
        if (data.city != null) user.city = data.city;
        if (data.state != null) user.state = data.state;
        if (data.country != null) user.country = data.country;

        if (data.companyEmail != null) user.companyEmail = data.companyEmail;
        if (data.companyPhoneNumber != null) user.companyPhoneNumber = data.companyPhoneNumber;
        if (data.website != null) user.website = data.website;
        if (data.industry != null) user.industry = data.industry;

        if (data.iban != null) user.iban = data.iban;
        if (data.bic != null) user.bic = data.bic;
        if (data.bankName != null) user.bankName = data.bankName;
        if (data.accountHolder != null) user.accountHolder = data.accountHolder;

        if (data.taxNumber != null) user.taxNumber = data.taxNumber;
        if (data.legalForm != null) user.legalForm = data.legalForm;

        if (data.employeeCount != null) user.employeeCount = data.employeeCount;
        if (data.customerCount != null) user.customerCount = data.customerCount;
        if (data.hourlyRate != null) user.hourlyRate = data.hourlyRate;
        if (data.priceListUrl != null) user.priceListUrl = data.priceListUrl;

        if (data.travelModel != null) user.travelModel = data.travelModel;
        if (data.travelFlatRate != null) user.travelFlatRate = data.travelFlatRate;
        if (data.travelKmRate != null) user.travelKmRate = data.travelKmRate;

        AuditLogEntity.log(
                user.id,
                "COMPANY_DATA_UPDATE",
                "User updated company metadata"
        );
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        List<UserRepresentation> users = keycloak.realm(REALM).users().search(email, true);

        if (users.isEmpty()) {
            return;
        }

        keycloak
                .realm(REALM)
                .users()
                .get(users.get(0).getId())
                .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
    }

    @Transactional
    public void deleteAccount(Long userId) {
        UserEntity user = UserEntity.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (user.keycloakId != null && !user.keycloakId.isBlank()) {
            keycloak.realm(REALM).users().get(user.keycloakId).remove();
        }

        user.status = UserStatus.DELETED;
        user.email = "deleted_" + user.id + "@handwerker.de";
        user.keycloakId = null;
        user.firstName = null;
        user.lastName = null;
        user.phoneNumber = null;
        user.profilePictureUrl = null;
        user.companyName = null;
        user.vatId = null;
        user.tradeRegisterNumber = null;

        user.street = null;
        user.houseNumber = null;
        user.zipCode = null;
        user.city = null;
        user.state = null;
        user.country = null;

        user.companyEmail = null;
        user.companyPhoneNumber = null;
        user.website = null;
        user.iban = null;
        user.bic = null;
        user.bankName = null;
        user.accountHolder = null;
        user.taxNumber = null;
        user.priceListUrl = null;

        AuditLogEntity.log(
                userId,
                "ACCOUNT_DELETED",
                "User account deleted and anonymized locally"
        );
    }

    @Transactional
    public UserEntity createCustomer(UserEntity customerData) {
        if (UserEntity.findByEmail(customerData.email) != null) {
            throw new BadRequestException("User with this email already exists");
        }

        customerData.status = UserStatus.ACTIVE;
        customerData.roles.add(UserRole.CUSTOMER);
        customerData.persist();

        AuditLogEntity.log(
                customerData.id,
                "CUSTOMER_CREATED",
                "Customer profile created by craftsman"
        );

        return customerData;
    }

    @Transactional
    public UserEntity updateCustomer(Long id, UserEntity data) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null || !customer.roles.contains(UserRole.CUSTOMER)) {
            throw new NotFoundException("Customer not found");
        }

        // Allow updating common fields if provided
        if (data.firstName != null) customer.firstName = data.firstName;
        if (data.lastName != null) customer.lastName = data.lastName;
        if (data.phoneNumber != null) customer.phoneNumber = data.phoneNumber;
        if (data.email != null) {
            // Prevent email collision with existing users
            UserEntity existing = UserEntity.findByEmail(data.email);
            if (existing != null && !existing.id.equals(id)) {
                throw new BadRequestException("Email already in use");
            }
            customer.email = data.email;
        }

        AuditLogEntity.log(
                customer.id,
                "CUSTOMER_UPDATED",
                "Customer data updated by craftsman"
        );

        return customer;
    }

    @Transactional
    public void deleteCustomer(Long id) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null || !customer.roles.contains(UserRole.CUSTOMER)) {
            throw new NotFoundException("Customer not found");
        }

        // Anonymize customer data similarly to deleting accounts
        customer.status = UserStatus.DELETED;
        customer.email = "deleted_customer_" + customer.id + "@handwerker.de";
        customer.keycloakId = null;
        customer.firstName = null;
        customer.lastName = null;
        customer.phoneNumber = null;
        customer.profilePictureUrl = null;

        AuditLogEntity.log(
                customer.id,
                "CUSTOMER_DELETED",
                "Customer deleted/anonymized by craftsman"
        );
    }

    public List<UserEntity> listCustomers() {
        return UserEntity.list(
                "from UserEntity u join u.roles r where r = ?1",
                UserRole.CUSTOMER
        );
    }

    public UserEntity getCustomerById(Long id) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null || !customer.roles.contains(UserRole.CUSTOMER)) {
            throw new NotFoundException("Customer not found");
        }

        return customer;
    }

    @Transactional
    public void updateTravelConfig(Long userId, Map<String, Object> data) {
        UserEntity user = UserEntity.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (data.containsKey("modell")) {
            user.travelModel = (String) data.get("modell");
        }

        if (data.containsKey("pauschale")) {
            Object pauschale = data.get("pauschale");
            if (pauschale != null) {
                user.travelFlatRate = new BigDecimal(pauschale.toString());
            }
        }

        if (data.containsKey("kmSatz")) {
            Object kmSatz = data.get("kmSatz");
            if (kmSatz != null) {
                user.travelKmRate = new BigDecimal(kmSatz.toString());
            }
        }

        if (data.containsKey("adresse")) {
            Map<String, Object> address = (Map<String, Object>) data.get("adresse");
            if (address != null) {
                if (address.containsKey("strasse")) {
                    user.street = (String) address.get("strasse");
                }
                if (address.containsKey("hausnummer")) {
                    user.houseNumber = (String) address.get("hausnummer");
                }
                if (address.containsKey("plz")) {
                    user.zipCode = (String) address.get("plz");
                }
                if (address.containsKey("ort")) {
                    user.city = (String) address.get("ort");
                }
                if (address.containsKey("bundesland")) {
                    user.state = (String) address.get("bundesland");
                }
                if (address.containsKey("land")) {
                    user.country = (String) address.get("land");
                }
            }
        }

        AuditLogEntity.log(
                user.id,
                "TRAVEL_CONFIG_UPDATE",
                "User updated travel configuration and address details"
        );
    }
}