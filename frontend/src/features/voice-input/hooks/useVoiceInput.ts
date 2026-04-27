import { useRef, useState } from "react";
import { startMicrophone } from "../services/microphoneService";

export const useVoiceInput = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [volume, setVolume] = useState(0);

  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const animationRef = useRef<number | undefined>(undefined);

  const start = async () => {
    const stream = await startMicrophone();

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

      setVolume(avg); // 👈 DAS ist deine “Schwingung”

      animationRef.current = requestAnimationFrame(update);
    };

    update();
    setIsRecording(true);
  };

  const stop = () => {
    animationRef.current && cancelAnimationFrame(animationRef.current);
    audioContextRef.current?.close();
    setIsRecording(false);
    setVolume(0);
  };

  const toggle = () => {
    isRecording ? stop() : start();
  };

  return { isRecording, volume, toggle };
};