package de.winfprojekt.craftvoice.offerservice.speechcapture;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.IOException;
import java.nio.file.Files;

@Path("/speech-capture")
@RolesAllowed({"OWNER"})
public class SpeechCaptureResource {

    @Inject
    DeepgramClient deepgramClient;

    public record TranscriptionResponse(String transkript) {}

    @POST
    @Path("/transcribe")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transcribe(@RestForm("audio") FileUpload audio) {
        if (audio == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Field 'audio' is missing")
                    .build();
        }

        String contentType = audio.contentType();
        if (contentType != null && !contentType.startsWith("audio/")) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Only audio files are accepted")
                    .build();
        }

        try {
            byte[] audioData = Files.readAllBytes(audio.uploadedFile());
            String transcript = deepgramClient.transcribe(audioData, contentType);

            if (transcript == null || transcript.trim().isEmpty()) {
                return Response.status(422) // Unprocessable Entity
                        .entity("Transcription failed: Transcript is empty")
                        .build();
            }

            return Response.ok(new TranscriptionResponse(transcript)).build();

        } catch (DeepgramException e) {
            return Response.status(502) // Bad Gateway
                    .entity("Deepgram error: " + e.getMessage())
                    .build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to read audio file: " + e.getMessage())
                    .build();
        }
    }
}
