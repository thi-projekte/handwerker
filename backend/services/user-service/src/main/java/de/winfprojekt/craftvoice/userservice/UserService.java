package de.winfprojekt.craftvoice.userservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

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
        if (user == null) throw new NotFoundException("User not found");

        try {
            String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + getExtension(file.fileName());
            Path path = Paths.get(storagePath, "profiles", fileName);
            Files.createDirectories(path.getParent());
            Files.copy(file.uploadedFile(), path);
            
            String url = "/api/users/profile-picture/" + fileName;
            user.profilePictureUrl = url;
            
            AuditLogEntity.log(user.id, "PROFILE_PICTURE_UPLOAD", "User uploaded a new profile picture: " + fileName);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Could not save profile picture", e);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
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
            throw new BadRequestException("Could not create user in Keycloak: " + response.readEntity(String.class));
        }

        String keycloakId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        
        userData.keycloakId = keycloakId;
        userData.status = UserStatus.PENDING;
        userData.roles.add(UserRole.OWNER);
        userData.persist();

        // Trigger verification email via Keycloak
        keycloak.realm(REALM).users().get(keycloakId).sendVerifyEmail();
        
        AuditLogEntity.log(userData.id, "REGISTRATION", "User registered via Keycloak");
    }

    @Transactional
    public UserEntity syncUserWithDatabase() {
        String keycloakId = jwt.getSubject();
        UserEntity user = UserEntity.findByKeycloakId(keycloakId);

        if (user == null) {
            user = new UserEntity();
            user.keycloakId = keycloakId;
            user.email = jwt.getClaim("email");
            user.firstName = jwt.getClaim("given_name");
            user.lastName = jwt.getClaim("family_name");
            user.status = UserStatus.ACTIVE;
            user.roles.add(UserRole.OWNER); // Default for first sync
            user.persist();
        } else {
            user.email = jwt.getClaim("email");
            user.firstName = jwt.getClaim("given_name");
            user.lastName = jwt.getClaim("family_name");
        }

        return user;
    }

    @Transactional
    public void updateProfile(Long userId, UserEntity data) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) throw new NotFoundException("User not found");

        if (data.firstName != null) user.firstName = data.firstName;
        if (data.lastName != null) user.lastName = data.lastName;
        if (data.phoneNumber != null) user.phoneNumber = data.phoneNumber;
        if (data.profilePictureUrl != null) user.profilePictureUrl = data.profilePictureUrl;
        
        // Sync names to Keycloak
        UserRepresentation kcUser = keycloak.realm(REALM).users().get(user.keycloakId).toRepresentation();
        if (data.firstName != null) kcUser.setFirstName(data.firstName);
        if (data.lastName != null) kcUser.setLastName(data.lastName);
        keycloak.realm(REALM).users().get(user.keycloakId).update(kcUser);

        AuditLogEntity.log(user.id, "PROFILE_UPDATE", "User updated personal data (synced with Keycloak)");
    }

    @Transactional
    public void updateCompanyData(Long userId, UserEntity data) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) throw new NotFoundException("User not found");

        if (data.companyName != null) user.companyName = data.companyName;
        if (data.vatId != null) user.vatId = data.vatId;
        if (data.tradeRegisterNumber != null) user.tradeRegisterNumber = data.tradeRegisterNumber;
        
        // Detailed Address
        if (data.street != null) user.street = data.street;
        if (data.houseNumber != null) user.houseNumber = data.houseNumber;
        if (data.zipCode != null) user.zipCode = data.zipCode;
        if (data.city != null) user.city = data.city;
        if (data.state != null) user.state = data.state;
        if (data.country != null) user.country = data.country;

        // Company Contact
        if (data.companyEmail != null) user.companyEmail = data.companyEmail;
        if (data.companyPhoneNumber != null) user.companyPhoneNumber = data.companyPhoneNumber;
        if (data.website != null) user.website = data.website;
        if (data.industry != null) user.industry = data.industry;

        // Banking Info
        if (data.iban != null) user.iban = data.iban;
        if (data.bic != null) user.bic = data.bic;
        if (data.bankName != null) user.bankName = data.bankName;
        if (data.accountHolder != null) user.accountHolder = data.accountHolder;

        // Tax Info
        if (data.taxNumber != null) user.taxNumber = data.taxNumber;
        if (data.legalForm != null) user.legalForm = data.legalForm;

        // Business Details
        if (data.employeeCount != null) user.employeeCount = data.employeeCount;
        if (data.customerCount != null) user.customerCount = data.customerCount;
        if (data.hourlyRate != null) user.hourlyRate = data.hourlyRate;
        if (data.priceListUrl != null) user.priceListUrl = data.priceListUrl;
        
        AuditLogEntity.log(user.id, "COMPANY_DATA_UPDATE", "User updated company metadata");
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        List<UserRepresentation> users = keycloak.realm(REALM).users().search(email, true);
        if (users.isEmpty()) return;

        keycloak.realm(REALM).users().get(users.get(0).getId()).executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
    }

    @Transactional
    public void deleteAccount(Long userId) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) throw new NotFoundException("User not found");

        // Delete in Keycloak
        keycloak.realm(REALM).users().get(user.keycloakId).remove();

        // Anonymize locally (GDPR)
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
        
        // Anonymize Address
        user.street = null;
        user.houseNumber = null;
        user.zipCode = null;
        user.city = null;
        user.state = null;
        user.country = null;

        // Anonymize Contact & Banking
        user.companyEmail = null;
        user.companyPhoneNumber = null;
        user.website = null;
        user.iban = null;
        user.bic = null;
        user.bankName = null;
        user.accountHolder = null;
        user.taxNumber = null;
        user.priceListUrl = null;
        
        AuditLogEntity.log(userId, "ACCOUNT_DELETED", "User account deleted (Synced with Keycloak)");
    }

    @Transactional
    public UserEntity createCustomer(UserEntity customerData) {
        if (UserEntity.findByEmail(customerData.email) != null) {
            throw new BadRequestException("User with this email already exists");
        }
        
        customerData.status = UserStatus.ACTIVE;
        customerData.roles.add(UserRole.CUSTOMER);
        customerData.persist();
        
        AuditLogEntity.log(customerData.id, "CUSTOMER_CREATED", "Customer profile created by craftsman");
        return customerData;
    }

    public List<UserEntity> listCustomers() {
        // Simple implementation: return all users with CUSTOMER role
        // In a real multi-tenant app, this would be filtered by company
        return UserEntity.list("from UserEntity u join u.roles r where r = ?1", UserRole.CUSTOMER);
    }
}
