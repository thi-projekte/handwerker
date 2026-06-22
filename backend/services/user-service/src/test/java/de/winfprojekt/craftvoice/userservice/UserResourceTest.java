package de.winfprojekt.craftvoice.userservice;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

/**
 * Mock-Tests für den UserResource-Controller.
 * Diese Tests benötigen KEINE Datenbank und KEIN Keycloak.
 */
@QuarkusTest
public class UserResourceTest {

    @InjectMock
    UserService userService;

    @Test
    public void testRegisterEndpoint() {
        UserResource.RegistrationRequest request = new UserResource.RegistrationRequest();
        request.email = "test@example.com";
        request.password = "password123";
        request.firstName = "Max";
        request.lastName = "Mustermann";

        given()
                .contentType("application/json")
                .body(request)
                .when().post("/api/users/register")
                .then()
                .statusCode(201);
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testMeEndpointWithMockedUser() {
        UserEntity mockUser = new UserEntity();
        mockUser.email = "test@example.com";
        mockUser.firstName = "Mock";
        mockUser.lastName = "User";
        
        // Da UserService gemockt ist, geben wir das Test-Objekt einfach zurück
        Mockito.when(userService.syncUserWithDatabase()).thenReturn(mockUser);

        given()
                .when().get("/api/users/me")
                .then()
                .statusCode(200)
                .body("email", is("test@example.com"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testCreateCustomer() {
        UserEntity customer = new UserEntity();
        customer.email = "customer@example.com";
        customer.firstName = "Kunde";
        
        Mockito.when(userService.createCustomer(Mockito.any())).thenReturn(customer);

        given()
                .contentType("application/json")
                .body(customer)
                .when().post("/api/users/customers")
                .then()
                .statusCode(201)
                .body("email", is("customer@example.com"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"CUSTOMER"})
    public void testCreateCustomerForbiddenForCustomerRole() {
        given()
                .contentType("application/json")
                .body(new UserEntity())
                .when().post("/api/users/customers")
                .then()
                .statusCode(403);
    }
}
