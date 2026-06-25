package de.winfprojekt.craftvoice.offerservice.processengine;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.winfprojekt.craftvoice.offerservice.processengine.dto.PeMessagePayload;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@QuarkusTest
class ProcessEngineRestClientWireMockTest {

    static WireMockServer wireMockServer;

    @Inject
    @RestClient
    ProcessEngineRestClient client;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        System.setProperty(
                "quarkus.rest-client.\"de.winfprojekt.craftvoice.offerservice.processengine.ProcessEngineRestClient\".url",
                wireMockServer.baseUrl() + "/engine-rest"
        );
    }

    @AfterAll
    static void teardown() {
        wireMockServer.stop();
    }

    @Test
    @TestSecurity(user = "process-engine", roles = {"process-engine"})
    void shouldCallCorrectEngineMessageEndpoint() {

        wireMockServer.stubFor(
                post(urlEqualTo("/engine-rest/message"))
                        .willReturn(aResponse().withStatus(200))
        );

        PeMessagePayload payload = new PeMessagePayload(
                "test",
                "bizKey",
                false
        );

        client.sendMessage(payload);

        wireMockServer.verify(
                postRequestedFor(urlEqualTo("/engine-rest/message"))
        );
    }
}