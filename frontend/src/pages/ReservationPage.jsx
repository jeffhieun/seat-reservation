import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { confirmReservation, expireReservation, getReservationById } from "../api/reservationApi";
import Navbar from "../components/Navbar";
import ReservationCard from "../components/ReservationCard";

function extractErrorMessage(err, fallback) {
  return err?.response?.data?.message
    || err?.response?.data?.error
    || err?.message
    || fallback;
}

function ReservationPage() {
  const { reservationId } = useParams();
  const navigate = useNavigate();

  const [reservation, setReservation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    const loadReservation = async () => {
      setLoading(true);
      setError("");
      setSuccessMessage("");

      try {
        const data = await getReservationById(reservationId);
        setReservation(data);
      } catch (err) {
        setError(extractErrorMessage(err, "Failed to load reservation details."));
      } finally {
        setLoading(false);
      }
    };

    loadReservation();
  }, [reservationId]);

  const handleConfirm = async () => {
    if (!reservation || actionLoading) {
      return;
    }

    setActionLoading(true);
    setError("");
    setSuccessMessage("");

    try {
      const updated = await confirmReservation(reservation.id);
      setReservation(updated);
      setSuccessMessage("Reservation confirmed successfully.");
    } catch (err) {
      setError(extractErrorMessage(err, "Failed to confirm reservation."));
    } finally {
      setActionLoading(false);
    }
  };

  const handleExpire = async () => {
    if (!reservation || actionLoading) {
      return;
    }

    setActionLoading(true);
    setError("");
    setSuccessMessage("");

    try {
      const updated = await expireReservation(reservation.id);
      setReservation(updated);
      setSuccessMessage("Reservation expired successfully.");
    } catch (err) {
      setError(extractErrorMessage(err, "Failed to expire reservation."));
    } finally {
      setActionLoading(false);
    }
  };

  const handleBack = () => {
    navigate("/seats", { replace: true, state: { shouldRefreshReservations: true } });
  };

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <div className="page-header">
          <h2>Reservation</h2>
          <button type="button" className="btn btn-secondary" onClick={handleBack}>
            Back to Seats
          </button>
        </div>

        {successMessage ? <p className="success-text">{successMessage}</p> : null}

        <ReservationCard
          reservation={reservation}
          loading={loading}
          error={error}
          actionLoading={actionLoading}
          onConfirm={handleConfirm}
          onExpire={handleExpire}
        />
      </section>
    </main>
  );
}

export default ReservationPage;

