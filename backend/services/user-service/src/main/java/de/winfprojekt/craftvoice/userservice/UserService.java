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
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject
    Keycloak keycloak;

    @Inject
    JsonWebToken jwt;

    private static final String REALM = "handwerker-realm";

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

        user.firstName = data.firstName;
        user.lastName = data.lastName;
        user.phoneNumber = data.phoneNumber;
        user.profilePictureUrl = data.profilePictureUrl;
        
        // Sync names to Keycloak
        UserRepresentation kcUser = keycloak.realm(REALM).users().get(user.keycloakId).toRepresentation();
        kcUser.setFirstName(data.firstName);
        kcUser.setLastName(data.lastName);
        keycloak.realm(REALM).users().get(user.keycloakId).update(kcUser);

        AuditLogEntity.log(user.id, "PROFILE_UPDATE", "User updated personal data (synced with Keycloak)");
    }

    @Transactional
    public void updateCompanyData(Long userId, UserEntity data) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) throw new NotFoundException("User not found");

        user.companyName = data.companyName;
        user.vatId = data.vatId;
        user.tradeRegisterNumber = data.tradeRegisterNumber;
        user.companyAddress = data.companyAddress;
        
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
        user.companyAddress = null;
        
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
