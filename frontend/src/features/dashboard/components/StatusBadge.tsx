type Props = {
  status: string;
};

export const StatusBadge = ({ status }: Props) => {
  return (
    <span className={`status-badge ${status}`}>
      {status === "offen" && "Ohne Rückmeldung"}
      {status === "antwort" && "Mit Rückmeldung"}
      {status === "unfertig" && "Nicht fertiggestellt"}
    </span>
  );
};