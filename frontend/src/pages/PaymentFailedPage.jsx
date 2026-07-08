import { useLocation, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";

function PaymentFailedPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const reservationId = location.state?.reservationId;

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <h2>Payment Failed</h2>
        <p className="error-text">Payment Failed</p>

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
        >
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
      </section>
    </main>
  );
}

export default PaymentFailedPage;
