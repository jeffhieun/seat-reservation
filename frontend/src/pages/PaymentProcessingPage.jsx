import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getReservationById } from "../api/reservationApi";
import {
  completePaymentSuccessInDevelopment,
  getPaymentById,
} from "../api/paymentApi";
import Navbar from "../components/Navbar";
import LoadingSpinner from "../components/common/LoadingSpinner";
import { getApiErrorMessage, normalizeApiError } from "../utils/apiError";

const POLL_INTERVAL_MS = 3000;
const TIMEOUT_MS = 60000;
const IS_DEVELOPMENT_MODE = import.meta.env.MODE === "development";

function PaymentProcessingPage() {
  const { paymentId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const startedAtRef = useRef(Date.now());
  const devCompletionTriggeredRef = useRef(false);

  const [payment, setPayment] = useState(null);
  const [error, setError] = useState("");
  const [timedOut, setTimedOut] = useState(false);
  const [loading, setLoading] = useState(true);
  const [isRetrying, setIsRetrying] = useState(false);

  useEffect(() => {
    if (timedOut) {
      return undefined;
    }

    let cancelled = false;
    let inFlight = false;

    const checkStatus = async () => {
      if (inFlight || cancelled) {
        return;
      }
      inFlight = true;
      try {
        const currentPayment = await getPaymentById(paymentId);
        if (cancelled) {
          return;
        }
        setPayment(currentPayment);
        setLoading(false);
        setError("");

        if (
          currentPayment?.status === "SUCCESS"
          && currentPayment?.reservationStatus === "CONFIRMED"
        ) {
          const reservation = await getReservationById(currentPayment.reservationId);
          if (cancelled) {
            return;
          }
          navigate("/payment/success", {
            replace: true,
            state: {
              reservation,
              payment: currentPayment,
            },
          });
          return;
        }

        if (currentPayment?.status === "FAILED") {
          navigate("/payment/failed", {
            replace: true,
            state: {
              payment: currentPayment,
              reservationId: currentPayment?.reservationId,
            },
          });
          return;
        }

        if (
          IS_DEVELOPMENT_MODE
          && !devCompletionTriggeredRef.current
          && currentPayment?.status === "PENDING"
        ) {
          devCompletionTriggeredRef.current = true;
          await completePaymentSuccessInDevelopment(paymentId);
          return;
        }

        if (Date.now() - startedAtRef.current >= TIMEOUT_MS) {
          setTimedOut(true);
        }
      } catch (err) {
        if (!cancelled) {
          const errorMessage = getApiErrorMessage(err, "Failed to check payment status. Please try again.");
          setError(errorMessage);
          setLoading(false);
        }
      } finally {
        inFlight = false;
      }
    };

    checkStatus();
    const timerId = window.setInterval(() => {
      if (!timedOut) {
        checkStatus();
      }
    }, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      window.clearInterval(timerId);
    };
  }, [navigate, paymentId, timedOut]);

  const handleRetry = () => {
    const reservationId = payment?.reservationId || location.state?.reservationId;
    if (reservationId) {
      navigate(`/payment/${reservationId}`, { replace: true, state: location.state });
      return;
    }
    navigate("/seats", { replace: true });
  };

  const handleRefreshStatus = async () => {
    setIsRetrying(true);
    setError("");
    try {
      const currentPayment = await getPaymentById(paymentId);
      setPayment(currentPayment);

      if (
        currentPayment?.status === "SUCCESS"
        && currentPayment?.reservationStatus === "CONFIRMED"
      ) {
        const reservation = await getReservationById(currentPayment.reservationId);
        navigate("/payment/success", {
          replace: true,
          state: {
            reservation,
            payment: currentPayment,
          },
        });
        return;
      }

      if (currentPayment?.status === "FAILED") {
        navigate("/payment/failed", {
          replace: true,
          state: {
            payment: currentPayment,
            reservationId: currentPayment?.reservationId,
          },
        });
        return;
      }

      if (currentPayment?.status === "PENDING") {
        setTimedOut(false);
        startedAtRef.current = Date.now();
      }
    } catch (err) {
      const errorMessage = getApiErrorMessage(err, "Failed to refresh payment status. Please try again.");
      setError(errorMessage);
    } finally {
      setIsRetrying(false);
    }
  };

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <h2>Processing Payment</h2>

        {loading && !error ? (
          <LoadingSpinner message="Processing payment... Please wait..." />
        ) : null}

        {payment?.status === "PENDING" && !timedOut ? (
          <div role="status" aria-live="polite" aria-busy="true">
            <p className="info-text">
              Payment is being processed...
              <br />
              <small>Payment ID: {payment.id}</small>
            </p>
          </div>
        ) : null}

        {error && !timedOut ? (
          <div role="alert">
            <p className="error-text">{error}</p>
            <button
              type="button"
              className="btn"
              onClick={handleRefreshStatus}
              disabled={isRetrying}
              aria-label="Refresh payment status"
            >
              {isRetrying ? "Checking..." : "Refresh Status"}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate("/seats")}
              style={{ marginTop: 12 }}
              aria-label="Return to seat selection"
            >
              Back to Reservations
            </button>
          </div>
        ) : null}

        {timedOut ? (
          <div role="alert">
            <p className="error-text">
              Payment confirmation is taking longer than expected.
              <br />
              <small>Payment ID: {payment?.id || paymentId}</small>
            </p>
            <button
              type="button"
              className="btn"
              onClick={handleRetry}
              aria-label="Try initiating payment again"
            >
              Retry
            </button>
            <button
              type="button"
              className="btn"
              onClick={handleRefreshStatus}
              disabled={isRetrying}
              style={{ marginTop: 12 }}
              aria-label="Check payment status again"
            >
              {isRetrying ? "Checking..." : "Refresh Status"}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate("/seats")}
              style={{ marginTop: 12 }}
              aria-label="Return to seat selection"
            >
              Back to Reservations
            </button>
          </div>
        ) : null}
      </section>
    </main>
  );
}

export default PaymentProcessingPage;
