import { useRef, useState } from "react";
import { startMicrophone } from "../services/microphoneService";

const OFFER_SERVICE_URL =
  import.meta.env.VITE_API_URL ||
  "https://offerservice-craftvoice.winfprojekt.de";

export const useVoiceInput = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [volume, setVolume] = useState(0);
  const [transcript, setTranscript] = useState("");
  const [audioBlobUrl, setAudioBlobUrl] = useState<string | null>(null);
  const [audioSegments, setAudioSegments] = useState<string[]>([]);
  const [audioBlobs, setAudioBlobs] = useState<Blob[]>([]);
  const [state, setState] = useState<
    "idle" | "recording" | "review" | "finished"
  >("idle");

  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const animationRef = useRef<number | undefined>(undefined);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  const start = async () => {
    setState("recording");
    const stream = await startMicrophone();

    // Audio Visualisation
    const audioContext = new AudioContext();
    const analyser = audioContext.createAnalyser();
    const source = audioContext.createMediaStreamSource(stream);

    source.connect(analyser);
    analyser.fftSize = 256;

    audioContextRef.current = audioContext;
    analyserRef.current = analyser;

    const dataArray = new Uint8Array(analyser.frequencyBinCount);

    const update = () => {
      if (!analyserRef.current) return;
      analyser.getByteFrequencyData(dataArray);
      const avg = dataArray.reduce((a, b) => a + b, 0) / dataArray.length;

      setVolume(avg);
      animationRef.current = requestAnimationFrame(update);
    };

    update();

    // 🎧 AUDIO RECORDING (Blob speichern)
    const mediaRecorder = new MediaRecorder(stream);
    mediaRecorderRef.current = mediaRecorder;
    chunksRef.current = [];

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) {
        chunksRef.current.push(e.data);
      }
    };

    mediaRecorder.onstop = () => {
      const blob = new Blob(chunksRef.current, {
        type: "audio/webm",
      });

      const url = URL.createObjectURL(blob);
      setAudioBlobUrl(url);
      console.log("Audio Blob ready:", blob);
    };

    mediaRecorder.start();
    setIsRecording(true);
  };

  // Aufnahme pausieren
  const pause = () => {
    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
    }

    if (!mediaRecorderRef.current) return;

    // 🔥 WICHTIG: Den Handler ZUERST definieren, BEVOR .stop() aufgerufen wird!
    mediaRecorderRef.current.onstop = () => {
      const url = createSegment();
      setAudioSegments((prev) => [...prev, url]);
    };

    if (mediaRecorderRef.current.state === "recording") {
      mediaRecorderRef.current.stop();
    }

    // AudioContext aufräumen / pausieren
    if (audioContextRef.current && audioContextRef.current.state !== "closed") {
      audioContextRef.current.close();
    }

    setIsRecording(false);
    setVolume(0);
    setState("review");
  };

  // Aufnahme fortsetzen
  const resume = async () => {
    const stream = await startMicrophone();

    const mediaRecorder = new MediaRecorder(stream);
    mediaRecorderRef.current = mediaRecorder;
    chunksRef.current = [];

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunksRef.current.push(e.data);
    };

    // Auch hier: Erst definieren, dann starten
    mediaRecorder.onstop = () => {
      const url = createSegment();
      setAudioSegments((prev) => [...prev, url]);
    };

    // Visualisierung für die Fortsetzung neu starten
    const audioContext = new AudioContext();
    const analyser = audioContext.createAnalyser();
    const source = audioContext.createMediaStreamSource(stream);
    source.connect(analyser);
    analyser.fftSize = 256;
    audioContextRef.current = audioContext;
    analyserRef.current = analyser;
    const dataArray = new Uint8Array(analyser.frequencyBinCount);

    const update = () => {
      analyser.getByteFrequencyData(dataArray);
      const avg = dataArray.reduce((a, b) => a + b, 0) / dataArray.length;
      setVolume(avg);
      animationRef.current = requestAnimationFrame(update);
    };
    update();

    mediaRecorder.start();
    setIsRecording(true);
    setState("recording");
  };

  // Aufnahme endgültig beenden
  const finalizeRecording = () => {
    if (mediaRecorderRef.current?.state === "recording") {
      // Falls finalize direkt aus der Aufnahme aufgerufen wird,
      // wollen wir das Haupt-onstop-Verhalten aus 'start' nutzen
      mediaRecorderRef.current.stop();
    }
    setState("review");
  };

  const createSegment = () => {
    const blob = new Blob(chunksRef.current, { type: "audio/webm" });
    // Blob für spätere Transkription speichern
    setAudioBlobs((prev) => [...prev, blob]);
    const url = URL.createObjectURL(blob);
    return url;
  };

  /**
   * Merged alle aufgenommenen Segmente zu einem einzigen Blob,
   * schickt ihn an /speech-capture/transcribe und gibt das Transkript zurück.
   */
  const transcribeAudio = async (): Promise<string> => {
    // Aktuellen (noch nicht gestoppten) Chunk ebenfalls einbeziehen
    const currentBlob =
      chunksRef.current.length > 0
        ? new Blob(chunksRef.current, { type: "audio/webm" })
        : null;

    const allBlobs = currentBlob
      ? [...audioBlobs, currentBlob]
      : [...audioBlobs];

    if (allBlobs.length === 0) {
      throw new Error("Keine Audioaufnahme vorhanden.");
    }

    // Alle Segmente zu einem Blob zusammenführen
    const mergedBlob = new Blob(allBlobs, { type: "audio/webm" });

    const formData = new FormData();
    formData.append("audio", mergedBlob, "aufnahme.webm");

    const response = await fetch(
      `${OFFER_SERVICE_URL}/speech-capture/transcribe`,
      {
        method: "POST",
        body: formData,
      },
    );

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(
        `Transkription fehlgeschlagen (${response.status}): ${errorText}`,
      );
    }

    const data = await response.json();
    return data.transkript as string;
  };

  const toggle = () => {
    if (state === "idle") {
      start();
    } else if (state === "recording") {
      pause();
    } else if (state === "review") {
      resume();
    }
  };

  const reset = () => {
    setState("idle");
    setTranscript("");
    setAudioSegments([]);
    setAudioBlobs([]);
    setAudioBlobUrl(null);
    if (animationRef.current) cancelAnimationFrame(animationRef.current);
    if (audioContextRef.current) audioContextRef.current.close();
  };

  return {
    isRecording,
    volume,
    toggle,
    transcript,
    setTranscript,
    audioSegments,
    audioBlobUrl,
    state,
    reset,
    resume,
    pause,
    finalizeRecording,
    transcribeAudio,
  };
};
