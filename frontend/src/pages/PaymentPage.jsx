import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getReservationById } from "../api/reservationApi";
import { completePayment, getPaymentById, initiatePayment } from "../api/paymentApi";
import Navbar from "../components/Navbar";
import { getApiErrorMessage } from "../utils/apiError";
import usePaymentStatus from "../hooks/usePaymentStatus";

function PaymentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { reservationId } = useParams();

  const [reservation, setReservation] = useState(
    location.state?.reservation || null
  );

  const [payment, setPayment] = useState(null);
  const [loadingReservation, setLoadingReservation] = useState(!location.state?.reservation);
  const [loadingPayment, setLoadingPayment] = useState(true);

  const [paying, setPaying] = useState(false);
  const [error, setError] = useState("");
  const [infoMessage, setInfoMessage] = useState("");
  const [showRetry, setShowRetry] = useState(false);
  const paymentIdFromQuery = new URLSearchParams(location.search).get("paymentId");
  const paymentStorageKey = `paymentId:${reservationId}`;

  useEffect(() => {
    const loadReservation = async () => {
      if (reservation) {
        return;
      }

      setLoadingReservation(true);
      setError("");

      try {
        const data = await getReservationById(reservationId);
        setReservation(data);
      } catch (err) {
        setError(getApiErrorMessage(err, "Failed to load reservation."));
      } finally {
        setLoadingReservation(false);
      }
    };

    loadReservation();
  }, [reservationId, reservation]);

  useEffect(() => {
    const loadPayment = async () => {
      setLoadingPayment(true);
      setError("");
      setInfoMessage("");

      try {
        const knownPaymentId = paymentIdFromQuery || window.sessionStorage.getItem(paymentStorageKey);

        if (knownPaymentId) {
          const existingPayment = await getPaymentById(knownPaymentId);
          setPayment(existingPayment);
          window.sessionStorage.setItem(paymentStorageKey, String(existingPayment.id));
          return;
        }

        const createdPayment = await initiatePayment(reservationId);
        setPayment(createdPayment);
        window.sessionStorage.setItem(paymentStorageKey, String(createdPayment.id));
      } catch (err) {
        setError(getApiErrorMessage(err, "Failed to load payment."));
      } finally {
        setLoadingPayment(false);
      }
    };

    loadPayment();
  }, [paymentIdFromQuery, paymentStorageKey, reservationId]);

  const refreshStatuses = useCallback(async () => {
    if (!payment?.id) {
      return null;
    }
    const [refreshedPayment, refreshedReservation] = await Promise.all([
      getPaymentById(payment.id),
      getReservationById(reservationId),
    ]);
    setPayment(refreshedPayment);
    setReservation(refreshedReservation);
    return {
      payment: refreshedPayment,
      reservation: refreshedReservation,
    };
  }, [payment?.id, reservationId]);

  const { isPolling, startPolling } = usePaymentStatus({
    intervalMs: 2000,
    timeoutMs: 60000,
    checkStatus: refreshStatuses,
    onSuccess: ({ payment: confirmedPayment, reservation: confirmedReservation }) => {
      setError("");
      setInfoMessage("Payment completed successfully.");
      setShowRetry(false);
      navigate("/success", {
        state: {
          reservation: confirmedReservation,
          payment: confirmedPayment,
        },
      });

      useEffect(() => {
        if (payment?.status === "SUCCESS" && reservation?.status === "CONFIRMED") {
          navigate("/success", {
            state: {
              reservation,
              payment,
            },
          });
        }
      }, [navigate, payment, reservation]);
    },
    onFailed: () => {
      setInfoMessage("");
      setError("Payment Failed");
      setShowRetry(true);
    },
    onTimeout: () => {
      setInfoMessage("");
      setError("Payment timeout. Please retry.");
      setShowRetry(true);
    },
    onError: (err) => {
      setInfoMessage("");
      setError(getApiErrorMessage(err, "Failed to check payment status."));
      setShowRetry(true);
    },
  });

  const handleCompletePayment = async () => {
    if (paying || isPolling || !payment?.id || payment?.status !== "PENDING") {
      return;
    }

    setError("");
    setShowRetry(false);
    setInfoMessage("Processing Payment...");
    setPaying(true);

    try {
      await completePayment(payment.id, "SUCCESS");
      startPolling();
    } catch (err) {
      setError(getApiErrorMessage(err, "Failed to complete payment."));
      setInfoMessage("");
      setShowRetry(true);
    } finally {
      setPaying(false);
    }
  };

  const handleRetry = async () => {
    setError("");
    setShowRetry(false);

    if (payment?.status === "PENDING") {
      setInfoMessage("Processing Payment...");
      startPolling();
      return;
    }

    setInfoMessage("");
    setError("Payment Failed");
    setShowRetry(true);
  };

  return (
    <main>
      <Navbar />

      <section className="page-content">

        <h2>Payment</h2>


        {loadingReservation && (
          <p>Loading reservation...</p>
        )}
        {loadingPayment && (
          <p>Loading payment...</p>
        )}


        {error && (
          <p className="error-text">
            {error}
          </p>
        )}
        {infoMessage && (
          <p className="info-text">
            {infoMessage}
          </p>
        )}


        {!loadingReservation && !reservation && (
          <p className="empty-state">
            No reservation data.
          </p>
        )}



        {reservation && (

          <div className="card details-card">

            <p>
              <strong>Reservation ID:</strong>
              {" "}
              {reservation.id}
            </p>


            <p>
              <strong>Seat Number:</strong>
              {" "}
              {reservation.seat_number || reservation.seatNumber}
            </p>


            <p>
              <strong>Reservation Status:</strong>
              {" "}
              {reservation.status}
            </p>


            {payment ? (
              <>
                <p>
                  <strong>Payment ID:</strong>
                  {" "}
                  {payment.id}
                </p>


                <p>
                  <strong>Payment Status:</strong>
                  {" "}
                  {payment.status}
                </p>


                <p>
                  <strong>Amount:</strong>
                  {" "}
                  {payment.amount}
                </p>
              </>
            ) : <p><strong>Payment Status:</strong> -</p>}



            <button
              className="btn"
              type="button"
              onClick={handleCompletePayment}
              disabled={paying || isPolling || loadingPayment || !payment || payment.status !== "PENDING"}
            >
              {paying ? "Completing Payment..." : isPolling ? "Processing Payment..." : "Complete Payment"}
            </button>

            {showRetry ? (
              <button
                className="btn btn-secondary"
                type="button"
                onClick={handleRetry}
                style={{ marginTop: 12 }}
              >
                Retry
              </button>
            ) : null}

            <button
              className="btn btn-secondary"
              type="button"
              onClick={() => navigate("/seats")}
              style={{ marginTop: 16 }}
            >
              ← Back to Seats
            </button>

          </div>

        )}

      </section>

    </main>
  );
}


export default PaymentPage;