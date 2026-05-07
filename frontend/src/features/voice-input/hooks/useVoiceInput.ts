import { useRef, useState } from "react";
import { startMicrophone } from "../services/microphoneService";

export const useVoiceInput = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [volume, setVolume] = useState(0);
  const [transcript, setTranscript] = useState("");
  const [audioBlobUrl, setAudioBlobUrl] = useState<string | null>(null);

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
      const avg =
        dataArray.reduce((a, b) => a + b, 0) / dataArray.length;

      setVolume(avg);
      animationRef.current = requestAnimationFrame(update);
    };

    update();

    // 🎧 AUDIO RECORDING (Blob speichern)
    const mediaRecorder = new MediaRecorder(stream);
    mediaRecorderRef.current = mediaRecorder;
    chunksRef.current = [];

    mediaRecorder.ondataavailable = (e) => {
      chunksRef.current.push(e.data);
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

  const stop = () => {
    animationRef.current && cancelAnimationFrame(animationRef.current);

    audioContextRef.current?.close();
    mediaRecorderRef.current?.stop();

    setIsRecording(false);
    setVolume(0);

    setState("review");
  };

  const toggle = () => {
    isRecording ? stop() : start();
  };
  const [state, setState] = useState<
    "idle" | "recording" | "review"
  >("idle");
  const reset = () => {
    setState("idle");
    setTranscript("");
    setAudioBlobUrl(null);
  };

  return {
    isRecording,
    volume,
    toggle,
    transcript,
    setTranscript,
    audioBlobUrl,
    state,
    reset,
  };
};