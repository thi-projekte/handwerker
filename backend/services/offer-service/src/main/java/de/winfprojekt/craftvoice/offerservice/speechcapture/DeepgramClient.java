package de.winfprojekt.craftvoice.offerservice.speechcapture;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class DeepgramClient {

    @ConfigProperty(name = "deepgram.api-key")
    String apiKey;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String transcribe(byte[] audioData, String contentType) throws DeepgramException {
        if (apiKey == null || apiKey.trim().isEmpty() || "changeme".equals(apiKey)) {
            throw new DeepgramException("Deepgram API key is not configured correctly.");
        }

        try {
            String requestContentType = contentType != null ? contentType : "audio/webm";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepgram.com/v1/listen?language=de&model=nova-2"))
                    .header("Authorization", "Token " + apiKey)
                    .header("Content-Type", requestContentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new DeepgramException("Deepgram returned HTTP status code " + response.statusCode() + ": " + response.body());
            }

            return parseTranscript(response.body());

        } catch (IOException e) {
            throw new DeepgramException("Failed to communicate with Deepgram: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeepgramException("Deepgram transcription was interrupted", e);
        }
    }

    String parseTranscript(String jsonResponse) throws DeepgramException {
        try {
            DeepgramResponse parsed = objectMapper.readValue(jsonResponse, DeepgramResponse.class);
            if (parsed == null || parsed.results() == null || parsed.results().channels() == null || parsed.results().channels().isEmpty()) {
                return "";
            }

            var channels = parsed.results().channels();
            var alternatives = channels.get(0).alternatives();
            if (alternatives == null || alternatives.isEmpty()) {
                return "";
            }

            String transcript = alternatives.get(0).transcript();
            return transcript != null ? transcript.trim() : "";
        } catch (IOException e) {
            throw new DeepgramException("Failed to parse Deepgram response: " + e.getMessage(), e);
        }
    }
}
