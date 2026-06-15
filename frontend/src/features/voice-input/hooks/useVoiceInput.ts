import { useRef, useState } from "react";
import { startMicrophone } from "../services/microphoneService";

export const useVoiceInput = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [volume, setVolume] = useState(0);
  const [transcript, setTranscript] = useState("");
  const [audioBlobUrl, setAudioBlobUrl] = useState<string | null>(null);
  const [audioSegments, setAudioSegments] = useState<string[]>([]);
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

      // 👉 optional: hier später Backend Upload möglich
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

    if (mediaRecorderRef.current?.state === "recording") {
      mediaRecorderRef.current.stop();
    }
    if (!mediaRecorderRef.current) return;

    mediaRecorderRef.current.onstop = () => {
      const url = createSegment();
      setAudioSegments((prev) => [...prev, url]);
    };

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

    mediaRecorder.onstop = () => {
      const url = createSegment();
      setAudioSegments((prev) => [...prev, url]);
    };

    mediaRecorder.start();
    setIsRecording(true);
    setState("recording");
  };

  // Aufnahme endgültig beenden und Blob erstellen
  const finalizeRecording = () => {
    mediaRecorderRef.current?.stop();
    setState("review");
  };
  const createSegment = () => {
    const blob = new Blob(chunksRef.current, { type: "audio/webm" });
    const url = URL.createObjectURL(blob);
    return url;
  };

  // 🔀 Schaltet zwischen Start und Stop um
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
  };

  return {
    isRecording,
    volume,
    toggle,
    transcript,
    setTranscript,
    audioSegments,
    state,
    reset,
    resume,
    pause,
    finalizeRecording,
  };
};
