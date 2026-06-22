package de.winfprojekt.craftvoice.documentservice.common;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@Path("/user")
@RegisterRestClient(configKey = "user-service")
public interface UserClient {

    @GET
    @Path("/customer/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    CustomerDto getCustomer(@PathParam("customerId") UUID customerId);

    @GET
    @Path("/company")
    @Produces(MediaType.APPLICATION_JSON)
    CompanyDto getCompany();
}