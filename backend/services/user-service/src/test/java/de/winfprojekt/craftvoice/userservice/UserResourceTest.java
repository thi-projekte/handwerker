package de.winfprojekt.craftvoice.userservice;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class UserResourceTest {

    @Test
    public void testPublicRegistrationEndpoint() {
        // Testet, ob der Endpoint existiert und Requests annimmt (auch wenn die Validierung ohne Daten fehlschlägt)
        given()
          .when().post("/api/users/register")
          .then()
             .statusCode(415); // Unsupported Media Type (da wir keinen Body senden)
    }

    @Test
    public void testMeEndpointIsProtected() {
        // Testet, ob der /me Endpoint gesichert ist (erwartet 401 Unauthorized ohne Token)
        given()
          .when().get("/api/users/me")
          .then()
             .statusCode(401);
    }
}
