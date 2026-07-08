import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getReservationById } from "../api/reservationApi";
import { completePayment, getPaymentById, initiatePayment } from "../api/paymentApi";
import Navbar from "../components/Navbar";
import { getApiErrorMessage } from "../utils/apiError";

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

  const handleCompletePayment = async () => {
    if (paying || !payment?.id || payment?.status !== "PENDING") {
      return;
    }

    setError("");
    setInfoMessage("");
    setPaying(true);

    try {
      const paymentResponse = await completePayment(payment.id, "SUCCESS");
      const refreshedPayment = await getPaymentById(payment.id);
      const refreshedReservation = await getReservationById(reservationId);

      setPayment(refreshedPayment);
      setReservation(refreshedReservation);

      if (
        paymentResponse?.status === "SUCCESS"
        && paymentResponse?.reservationStatus === "CONFIRMED"
      ) {
        setInfoMessage("Payment completed successfully.");
      } else {
        setInfoMessage("Payment did not complete successfully.");
      }

    } catch (err) {
      setError(getApiErrorMessage(err, "Failed to complete payment."));
    } finally {
      setPaying(false);
    }
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
              disabled={paying || loadingPayment || !payment || payment.status !== "PENDING"}
            >
              {paying ? "Completing Payment..." : "Complete Payment"}
            </button>


          </div>

        )}

      </section>

    </main>
  );
}


export default PaymentPage;