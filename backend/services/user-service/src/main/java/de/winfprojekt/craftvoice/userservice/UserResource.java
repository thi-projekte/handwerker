package de.winfprojekt.craftvoice.userservice;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.HashMap;
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

    @POST
    @Path("/register")
    @PermitAll
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

    @GET
    @Path("/profile/hourly-rate")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response getHourlyRate() {
        UserEntity user = userService.syncUserWithDatabase();

        Map<String, Object> response = new HashMap<>();
        response.put("stundensatz", user.hourlyRate != null ? user.hourlyRate : 0.0);

        return Response.ok(response).build();
    }

    @GET
    @Path("/profile/travel-config")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response getTravelConfig() {
        UserEntity user = userService.syncUserWithDatabase();

        String formattedAddress = String.format(
                "%s %s, %s %s",
                user.street != null ? user.street : "",
                user.houseNumber != null ? user.houseNumber : "",
                user.zipCode != null ? user.zipCode : "",
                user.city != null ? user.city : ""
        ).trim();

        Map<String, Object> response = new HashMap<>();
        response.put("modell", user.travelModel != null ? user.travelModel : "PAUSCHALE");
        response.put("pauschale", user.travelFlatRate);
        response.put("kmSatz", user.travelKmRate);
        response.put("adresse", formattedAddress);

        return Response.ok(response).build();
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
        UserEntity updatedUser = userService.updateProfile(getUserId(), data);

        return Response.ok(updatedUser).build();
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
    @PermitAll
    public Response initiateReset(Map<String, String> request) {
        userService.initiatePasswordReset(request.get("email"));

        return Response
                .ok("If the email exists, a reset instruction has been sent via Keycloak")
                .build();
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

    @GET
    @Path("/customers/{id}")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public UserEntity getCustomer(@PathParam("id") Long id) {
        return userService.getCustomerById(id);
    }

    @PUT
    @Path("/customers/{id}")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public UserEntity updateCustomer(@PathParam("id") Long id, UserEntity data) {
        return userService.updateCustomer(id, data);
    }

    @DELETE
    @Path("/customers/{id}")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response deleteCustomer(@PathParam("id") Long id) {
        userService.deleteCustomer(id);
        return Response.ok().build();
    }

    private Long getUserId() {
        UserEntity user = userService.syncUserWithDatabase();

        if (user == null) {
            throw new NotFoundException("User not synced");
        }

        return user.id;
    }

    public static class RegistrationRequest {
        public String email;
        public String password;
        public String firstName;
        public String lastName;
    }
}