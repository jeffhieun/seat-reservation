import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getSeats } from "../api/seatApi";
import { getUserReservations, reserveSeat } from "../api/reservationApi";
import Navbar from "../components/Navbar";
import SeatGrid from "../components/SeatGrid";
import ReservationTabs from "../components/ReservationTabs";
import ReservationTable from "../components/ReservationTable";

const AUTO_REFRESH_MS = 15000;

function extractErrorMessage(err, fallbackMessage) {
  return err?.response?.data?.message
    || err?.response?.data?.error
    || err?.message
    || fallbackMessage;
}

function SeatsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [bookingSeatId, setBookingSeatId] = useState(null);
  const [reservations, setReservations] = useState([]);
  const [reservationsLoading, setReservationsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("PENDING_PAYMENT");

  const loadSeats = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getSeats();
      setSeats(Array.isArray(data) ? data : []);
    } catch (err) {
      const message = extractErrorMessage(err, "Failed to load seats.");
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const loadReservations = async () => {
    setReservationsLoading(true);

    try {
      const data = await getUserReservations();
      setReservations(Array.isArray(data) ? data : []);
    } catch (err) {
      const message = extractErrorMessage(err, "Failed to load reservations.");
      setError(message);
      setReservations([]);
    } finally {
      setReservationsLoading(false);
    }
  };

  useEffect(() => {
    loadSeats();
    loadReservations();
  }, []);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      loadSeats();
      loadReservations();
    }, AUTO_REFRESH_MS);

    return () => window.clearInterval(intervalId);
  }, []);

  useEffect(() => {
    if (location.state?.shouldRefreshReservations) {
      loadSeats();
      loadReservations();
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, location.state, navigate]);

  const handleSeatSelect = async (seat) => {
    const accepted = window.confirm(`Do you want to book seat ${seat.seatNumber}?`);
    if (!accepted) {
      return;
    }

    setError("");
    setSuccessMessage("");
    setBookingSeatId(seat.id);

    try {
      const reservation = await reserveSeat(seat.id);
      setSuccessMessage(`Seat ${seat.seatNumber} reserved successfully.`);
      await loadSeats();
      await loadReservations();
      navigate(`/payment/${reservation.id}`, {
        state: { reservation },
      });
    } catch (err) {
      const isDuplicateReservation = err?.response?.status === 409
        && (err?.response?.data?.message === "You already have an active reservation for this seat."
          || err?.response?.data?.message === "You already reserved this seat.");
      const message = isDuplicateReservation
        ? "You already have an active reservation for this seat."
        : extractErrorMessage(err, "Failed to reserve seat.");
      setError(message);
      if (!isDuplicateReservation) {
        await loadSeats();
        await loadReservations();
      }
    } finally {
      setBookingSeatId(null);
    }
  };

  const handlePayReservation = (reservation) => {
    if (!reservation?.id) {
      return;
    }

    navigate(`/payment/${reservation.id}`, {
      state: { reservation },
    });
  };

  const handleViewReservation = (reservation) => {
    if (!reservation?.id) {
      return;
    }
    navigate(`/reservations/${reservation.id}`);
  };

  const pendingReservations = reservations.filter((reservation) => reservation?.status === "PENDING_PAYMENT");
  const confirmedReservations = reservations.filter((reservation) => reservation?.status === "CONFIRMED");
  const expiredReservations = reservations.filter((reservation) => reservation?.status === "EXPIRED");

  const filteredReservations =
    activeTab === "PENDING_PAYMENT"
      ? pendingReservations
      : activeTab === "CONFIRMED"
        ? confirmedReservations
        : expiredReservations;

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <section className="seats-block">
          <div className="page-header">
            <h2>Available Seats</h2>
            <button className="btn" type="button" onClick={loadSeats} disabled={loading || bookingSeatId !== null}>
              {loading ? "Refreshing..." : "Refresh"}
            </button>
          </div>

          {successMessage ? <p className="success-text">{successMessage}</p> : null}
          {error ? <p className="error-text">{error}</p> : null}

          {bookingSeatId !== null ? <p className="info-text">Booking seat...</p> : null}

          <div className="seats-grid-wrap">
            {loading ? <p>Loading seats...</p> : <SeatGrid seats={seats} onSeatSelect={handleSeatSelect} />}
          </div>
        </section>

        <section className="reservations-block">
          <h2>Reservations</h2>

          <ReservationTabs
            activeTab={activeTab}
            onTabChange={setActiveTab}
            pendingCount={pendingReservations.length}
            confirmedCount={confirmedReservations.length}
            expiredCount={expiredReservations.length}
          />

          <div className="reservations-table-wrap">
            {reservationsLoading ? (
              <p>Loading reservations...</p>
            ) : (
              <ReservationTable
                reservations={filteredReservations}
                activeTab={activeTab}
                onPay={handlePayReservation}
                onViewDetails={handleViewReservation}
              />
            )}
          </div>
        </section>
      </section>
    </main>
  );
}

export default SeatsPage;
