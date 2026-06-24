package de.winfprojekt.craftvoice.documentservice.client.user;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/users")
@RegisterRestClient(configKey = "user-service")
public interface UserClient {

    @GET
    @Path("/me")
    UserDto getMe(
            @HeaderParam("Authorization") String authorizationHeader
    );

    @GET
    @Path("/customers/{id}")
    UserDto getCustomer(
            @PathParam("id") String customerId,
            @HeaderParam("Authorization") String authorizationHeader
    );
}