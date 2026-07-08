import { useEffect, useState } from "react";

export default function useReservationPolling({
  enabled = true,
  intervalMs = 5000,
  pollFn,
}) {
  const [isPolling, setIsPolling] = useState(false);

  useEffect(() => {
    if (!enabled || typeof pollFn !== "function") {
      return undefined;
    }

    let cancelled = false;
    let inFlight = false;

    const poll = async () => {
      if (inFlight || cancelled) {
        return;
      }
      inFlight = true;
      setIsPolling(true);
      try {
        await pollFn();
      } finally {
        inFlight = false;
        if (!cancelled) {
          setIsPolling(false);
        }
      }
    };

    const timerId = window.setInterval(poll, intervalMs);

    return () => {
      cancelled = true;
      window.clearInterval(timerId);
    };
  }, [enabled, intervalMs, pollFn]);

  return { isPolling };
}
