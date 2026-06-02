package de.winfprojekt.craftvoice.offerservice.speechcapture;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class SpeechCaptureResourceTest {

    @InjectMock
    DeepgramClient deepgramClient;

    @Test
    public void testTranscribeSuccess() throws DeepgramException {
        Mockito.when(deepgramClient.transcribe(Mockito.any(byte[].class), Mockito.any(String.class)))
                .thenReturn("Das ist ein echtes Transkript.");

        given()
            .multiPart("audio", "audio.webm", "dummy-audio-content".getBytes(), "audio/webm")
        .when()
            .post("/speech-capture/transcribe")
        .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON)
            .body("transkript", is("Das ist ein echtes Transkript."));
    }

    @Test
    public void testTranscribeEmptyTranscript() throws DeepgramException {
        Mockito.when(deepgramClient.transcribe(Mockito.any(byte[].class), Mockito.any(String.class)))
                .thenReturn("");

        given()
            .multiPart("audio", "audio.webm", "dummy-audio-content".getBytes(), "audio/webm")
        .when()
            .post("/speech-capture/transcribe")
        .then()
            .statusCode(422);
    }

    @Test
    public void testTranscribeDeepgramError() throws DeepgramException {
        Mockito.when(deepgramClient.transcribe(Mockito.any(byte[].class), Mockito.any(String.class)))
                .thenThrow(new DeepgramException("API key invalid or connection timed out"));

        given()
            .multiPart("audio", "audio.webm", "dummy-audio-content".getBytes(), "audio/webm")
        .when()
            .post("/speech-capture/transcribe")
        .then()
            .statusCode(502);
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
