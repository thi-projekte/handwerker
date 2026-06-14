package de.winfprojekt.craftvoice.userservice;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.mockito.Mockito;

@ApplicationScoped
public class MockTokenProducer {

    @Produces
    @Mock
    @ApplicationScoped
    public JsonWebToken mockToken() {
        return Mockito.mock(JsonWebToken.class);
    }
}
