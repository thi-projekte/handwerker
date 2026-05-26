package de.winfprojekt.craftvoice.offerservice.speechcapture;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class SpeechCaptureResourceTest {

    @Test
    public void testTranscribeSuccess() {
        given()
            .multiPart("audio", "audio.webm", "dummy-audio-content".getBytes(), "audio/webm")
        .when()
            .post("/speech-capture/transcribe")
        .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON)
            .body("transkript", is("Platzhalter-Transkript — Deepgram noch nicht integriert"));
    }

    @Test
    public void testTranscribeMissingAudio() {
        given()
            .multiPart("notAudio", "value")
        .when()
            .post("/speech-capture/transcribe")
        .then()
            .statusCode(400);
    }

    @Test
    public void testTranscribeInvalidMimeType() {
        given()
            .multiPart("audio", "test.txt", "some text".getBytes(), "text/plain")
        .when()
            .post("/speech-capture/transcribe")
        .then()
            .statusCode(415);
    }

    @Test
    public void testTranscribeOctetStreamRejected() {
        given()
            .multiPart("audio", "audio.bin", "some bytes".getBytes(), "application/octet-stream")
        .when()
            .post("/speech-capture/transcribe")
        .then()
            .statusCode(415);
    }
}
