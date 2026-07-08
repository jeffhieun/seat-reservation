import { useEffect, useMemo, useState } from "react";

function toTimestamp(value) {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.getTime();
}

function formatRemaining(seconds) {
  const safeSeconds = Math.max(0, seconds);
  const minutes = Math.floor(safeSeconds / 60);
  const remain = safeSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remain).padStart(2, "0")}`;
}

export default function useCountdown(expiresAt) {
  const expiresAtTs = useMemo(() => toTimestamp(expiresAt), [expiresAt]);
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    if (!expiresAtTs) {
      return undefined;
    }

    const timerId = window.setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, [expiresAtTs]);

  if (!expiresAtTs) {
    return { remainingSeconds: null, label: "-" };
  }

  const remainingSeconds = Math.ceil((expiresAtTs - now) / 1000);

  if (remainingSeconds <= 0) {
    return { remainingSeconds: 0, label: "Expired" };
  }

  return {
    remainingSeconds,
    label: formatRemaining(remainingSeconds),
  };
}
