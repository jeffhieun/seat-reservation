import { useCallback, useEffect, useRef, useState } from "react";

export default function usePaymentStatus({
  intervalMs = 2000,
  timeoutMs = 60000,
  checkStatus,
  onSuccess,
  onFailed,
  onTimeout,
  onError,
}) {
  const [isPolling, setIsPolling] = useState(false);
  const startedAtRef = useRef(0);
  const inFlightRef = useRef(false);

  const stopPolling = useCallback(() => {
    setIsPolling(false);
  }, []);

  const startPolling = useCallback(() => {
    startedAtRef.current = Date.now();
    setIsPolling(true);
  }, []);

  useEffect(() => {
    if (!isPolling || typeof checkStatus !== "function") {
      return undefined;
    }

    let cancelled = false;

    const tick = async () => {
      if (cancelled || inFlightRef.current) {
        return;
      }
      inFlightRef.current = true;

      try {
        const status = await checkStatus();
        if (cancelled || !status) {
          return;
        }

        const paymentStatus = status.payment?.status;
        const reservationStatus = status.reservation?.status;

        if (paymentStatus === "SUCCESS" && reservationStatus === "CONFIRMED") {
          stopPolling();
          onSuccess?.(status);
          return;
        }

        if (paymentStatus === "FAILED" || reservationStatus === "EXPIRED") {
          stopPolling();
          onFailed?.(status);
          return;
        }

        if (Date.now() - startedAtRef.current >= timeoutMs) {
          stopPolling();
          onTimeout?.(status);
        }
      } catch (error) {
        stopPolling();
        onError?.(error);
      } finally {
        inFlightRef.current = false;
      }
    };

    tick();
    const timerId = window.setInterval(tick, intervalMs);
    return () => {
      cancelled = true;
      window.clearInterval(timerId);
    };
  }, [checkStatus, intervalMs, isPolling, onError, onFailed, onSuccess, onTimeout, stopPolling, timeoutMs]);

  return {
    isPolling,
    startPolling,
    stopPolling,
  };
}
