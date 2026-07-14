import { useLocation, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";

function PaymentFailedPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const reservationId = location.state?.reservationId;
  const payment = location.state?.payment;

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <h2>Payment Failed</h2>
        
        {payment?.failureReason && (
          <p className="error-text">
            {payment.failureReason}
          </p>
        )}
        
        {!payment?.failureReason && (
          <p className="error-text">Payment Failed</p>
        )}

        <button
          type="button"
          className="btn"
          onClick={() => {
            if (reservationId) {
              navigate(`/payment/${reservationId}`, { replace: true });
              return;
            }
            navigate("/seats", { replace: true });
          }}
          aria-label="Retry payment"
        >
          Retry Payment
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

        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => navigate("/seats")}
          style={{ marginTop: 12 }}
          aria-label="Return to seats page"
        >
          Back to Seats
        </button>
      </section>
    </main>
  );
}

export default PaymentFailedPage;
