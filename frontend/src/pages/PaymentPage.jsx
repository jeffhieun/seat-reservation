import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getReservationById } from "../api/reservationApi";
import { initiatePayment } from "../api/paymentApi";
import Navbar from "../components/Navbar";
import { getApiErrorMessage } from "../utils/apiError";

function PaymentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { reservationId } = useParams();

  const [reservation, setReservation] = useState(
    location.state?.reservation || null
  );

  const [loadingReservation, setLoadingReservation] = useState(!location.state?.reservation);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState("");

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

  const handlePay = async () => {
    if (paying) {
      return;
    }

    setError("");
    setPaying(true);

    try {
      const payment = await initiatePayment(reservationId);
      window.sessionStorage.setItem(`paymentId:${reservationId}`, String(payment.id));
      navigate(`/payment/processing/${payment.id}`, {
        state: {
          reservationId,
          reservation,
        },
      });
    } catch (err) {
      setError(getApiErrorMessage(err, "Failed to initiate payment."));
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
        {error && (
          <p className="error-text">
            {error}
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


            <button
              className="btn"
              type="button"
              onClick={handlePay}
              disabled={paying}
            >
              {paying ? "Processing..." : "Pay"}
            </button>

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