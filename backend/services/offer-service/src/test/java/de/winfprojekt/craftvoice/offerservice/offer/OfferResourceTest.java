package de.winfprojekt.craftvoice.offerservice.offer;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static de.winfprojekt.craftvoice.offerservice.offer.Offer.STATUS_ERFASST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class OfferResourceTest {

    @InjectMock
    ProcessEngineClient processEngineClient;

    @Test
    void shouldCreateOffer() {

        Mockito.doNothing()
                .when(processEngineClient)
                .sendAngebotPayload(any(), any(), any(), any());

        Number offerId = given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "customerId": 1,
                  "sprachschnipsel": "Kunde möchte Badrenovierung"
                }
                """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long id = offerId.longValue();

        Offer offer = Offer.findById(id);

        assertNotNull(offer);
        assertTrue(offer.businessKey.startsWith("angebot-"));
        assertNotNull(offer.annahmeToken);
        assertEquals(STATUS_ERFASST, offer.status);

        List<OfferStatusHistory> history =
                OfferStatusHistory.find("offer.id", id).list();

        assertEquals(1, history.size());
        assertEquals(STATUS_ERFASST, history.get(0).status);

        ArgumentCaptor<String> businessKeyCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Long> customerIdCaptor =
                ArgumentCaptor.forClass(Long.class);

        ArgumentCaptor<String> sprachschnipselCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Object> vorlageCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(processEngineClient, times(1)).sendAngebotPayload(
                businessKeyCaptor.capture(),
                customerIdCaptor.capture(),
                sprachschnipselCaptor.capture(),
                vorlageCaptor.capture()
        );

        assertEquals(1L, customerIdCaptor.getValue());

        assertEquals(
                "Kunde möchte Badrenovierung",
                sprachschnipselCaptor.getValue()
        );

        assertEquals(
                offer.businessKey,
                businessKeyCaptor.getValue()
        );


       verify(processEngineClient).sendAngebotPayload(
                any(),
                customerIdCaptor.capture(),
                any(),
                any()
        );

        assertEquals(1L, customerIdCaptor.getValue());
    }

    @Test
    void shouldReturn400WhenSprachschnipselMissing() {

        given()
                .contentType(ContentType.JSON)
                .body("""
        {
          "customerId": 1
        }
        """)
                .when()
                .post("/offers")
                .then()
                .statusCode(400);
    }

}
