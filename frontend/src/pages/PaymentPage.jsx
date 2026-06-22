import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getReservationById } from "../api/reservationApi";
import { initiatePayment } from "../api/paymentApi";
import Navbar from "../components/Navbar";

function PaymentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { reservationId } = useParams();

  const [reservation, setReservation] = useState(
    location.state?.reservation || null
  );

  const [payment, setPayment] = useState(null);
  const [loadingReservation, setLoadingReservation] = useState(
    !location.state?.reservation
  );

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
        setError(
          err?.response?.data?.error ||
          err?.message ||
          "Failed to load reservation."
        );
      } finally {
        setLoadingReservation(false);
      }
    };

    loadReservation();
  }, [reservationId, reservation]);


  const handlePayNow = async () => {
    if (paying) {
      return;
    }

    setError("");
    setPaying(true);

    try {
      const paymentResponse = await initiatePayment(reservationId);

      console.log("Payment created:", paymentResponse);

      setPayment(paymentResponse);

      navigate("/success", {
        state: {
          reservation,
          payment: paymentResponse,
        },
      });

    } catch (err) {
      console.error("Payment error:", err);

      setError(
        err?.response?.data?.error ||
        err?.message ||
        "Payment failed."
      );

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
              {reservation.seat_number}
            </p>


            <p>
              <strong>Reservation Status:</strong>
              {" "}
              {reservation.status}
            </p>


            {payment && (
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
            )}



            <button
              className="btn"
              type="button"
              onClick={handlePayNow}
              disabled={paying}
            >
              {paying ? "Processing..." : "Pay Now"}
            </button>


          </div>

        )}

      </section>

    </main>
  );
}


export default PaymentPage;