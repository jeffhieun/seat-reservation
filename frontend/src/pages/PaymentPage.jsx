import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getReservationById } from "../api/reservationApi";
import { initiatePayment } from "../api/paymentApi";
import Navbar from "../components/Navbar";

function PaymentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { reservationId } = useParams();

  const [reservation, setReservation] = useState(location.state?.reservation || null);
  const [payment, setPayment] = useState(null);
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
        const message = err?.response?.data?.error || err?.message || "Failed to load reservation.";
        setError(message);
      } finally {
        setLoadingReservation(false);
      }
    };

    loadReservation();
  }, [reservationId, reservation]);

  const handlePayNow = async () => {
    setError("");
    setPaying(true);

    try {
      const data = await initiatePayment(reservationId);
      setPayment(data);
      navigate("/success", {
        state: {
          reservation,
          payment: data,
        },
      });
    } catch (err) {
      const message = err?.response?.data?.error || err?.message || "Payment failed.";
      setError(message);
    } finally {
      setPaying(false);
    }
  };

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <h2>Payment</h2>

        {loadingReservation ? <p>Loading reservation...</p> : null}
        {error ? <p className="error-text">{error}</p> : null}

        {!loadingReservation && !reservation ? <p className="empty-state">No reservation data.</p> : null}

        {reservation ? (
          <div className="card details-card">
            <p><strong>Reservation ID:</strong> {reservation.id}</p>
            <p><strong>Seat Number:</strong> {reservation.seat_number}</p>
            <p><strong>Reservation Status:</strong> {reservation.status}</p>
            <p><strong>Amount:</strong> {payment?.amount || "N/A"}</p>

            <button className="btn" type="button" onClick={handlePayNow} disabled={paying}>
              {paying ? "Processing..." : "Pay Now"}
            </button>
          </div>
        ) : null}
      </section>
    </main>
  );
}

export default PaymentPage;

