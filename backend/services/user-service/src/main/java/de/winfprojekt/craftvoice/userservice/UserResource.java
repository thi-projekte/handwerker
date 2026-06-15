package de.winfprojekt.craftvoice.userservice;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.util.List;
import java.util.Map;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity identity;

    @POST
    @Path("/register")
    @jakarta.annotation.security.PermitAll
    public Response register(RegistrationRequest request) {
        UserEntity user = new UserEntity();
        user.email = request.email;
        user.firstName = request.firstName;
        user.lastName = request.lastName;
        userService.register(user, request.password);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/me")
    @Authenticated
    public UserEntity me() {
        return userService.syncUserWithDatabase();
    }

    @POST
    @Path("/profile-picture")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    public Response uploadProfilePicture(@RestForm("file") FileUpload file) {
        String url = userService.uploadProfilePicture(getUserId(), file);
        return Response.ok(Map.of("url", url)).build();
    }

    @PUT
    @Path("/profile")
    @Authenticated
    public Response updateProfile(UserEntity data) {
        userService.updateProfile(getUserId(), data);
        return Response.ok().build();
    }

    @PUT
    @Path("/company")
    @RolesAllowed("OWNER")
    public Response updateCompany(UserEntity data) {
        userService.updateCompanyData(getUserId(), data);
        return Response.ok().build();
    }

    @POST
    @Path("/password-reset/initiate")
    public Response initiateReset(Map<String, String> request) {
        userService.initiatePasswordReset(request.get("email"));
        return Response.ok("If the email exists, a reset instruction has been sent via Keycloak").build();
    }

    @DELETE
    @RolesAllowed("OWNER")
    public Response deleteAccount() {
        userService.deleteAccount(getUserId());
        return Response.ok("Account deleted in Keycloak and anonymized locally").build();
    }

    @POST
    @Path("/customers")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response createCustomer(UserEntity customer) {
        UserEntity created = userService.createCustomer(customer);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/customers")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public List<UserEntity> listCustomers() {
        return userService.listCustomers();
    }

    private Long getUserId() {
        UserEntity user = UserEntity.findByKeycloakId(jwt.getSubject());
        if (user == null) throw new NotFoundException("User not synced");
        return user.id;
    }

    public static class RegistrationRequest {
        public String email;
        public String password;
        public String firstName;
        public String lastName;
    }
}
