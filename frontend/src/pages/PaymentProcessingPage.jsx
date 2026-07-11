import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getReservationById } from "../api/reservationApi";
import {
  completePaymentSuccessInDevelopment,
  getPaymentById,
} from "../api/paymentApi";
import Navbar from "../components/Navbar";
import LoadingSpinner from "../components/common/LoadingSpinner";
import { getApiErrorMessage } from "../utils/apiError";

const POLL_INTERVAL_MS = 2000;
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
          setError(getApiErrorMessage(err, "Failed to check payment status."));
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

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <h2>Processing Payment</h2>

        {loading && !error ? <LoadingSpinner message="Processing payment... Please wait..." /> : null}
        {payment?.status === "PENDING" && !timedOut ? <p className="info-text">Processing payment...</p> : null}
        {error ? <p className="error-text">{error}</p> : null}

        {timedOut ? (
          <>
            <p className="error-text">Payment timed out. Please retry.</p>
            <button type="button" className="btn" onClick={handleRetry}>
              Retry
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate("/seats")}
              style={{ marginTop: 12 }}
            >
              Back to Reservations
            </button>
          </>
        ) : null}
      </section>
    </main>
  );
}

export default PaymentProcessingPage;
