package de.winfprojekt.craftvoice.offerservice.speechcapture;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/speech-capture")
public class SpeechCaptureResource {

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

        TranscriptionResponse responseBody = new TranscriptionResponse(
                "Platzhalter-Transkript — Deepgram noch nicht integriert"
        );

        return Response.ok(responseBody).build();
    }
}
