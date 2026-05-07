import MicIcon from "@/assets/icons/mic.svg";

type Props = {
  isRecording: boolean;
  volume: number;
  onClick: () => void;
};

export const MicButton = ({ isRecording, volume, onClick }: Props) => {
  return (
    <button
      className={`mic-button ${isRecording ? "active" : ""}`}
      onClick={onClick}
      style={{
        transform: `scale(${1 + volume / 200})`,
      }}
    >
      <img src={MicIcon} alt="Microphone" />
    </button>
  );
};