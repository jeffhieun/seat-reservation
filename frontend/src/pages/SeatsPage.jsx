import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getSeats } from "../api/seatApi";
import { getUserReservations, reserveSeat } from "../api/reservationApi";
import Navbar from "../components/Navbar";
import SeatGrid from "../components/SeatGrid";
import ReservationTabs from "../components/ReservationTabs";
import ReservationTable from "../components/ReservationTable";

function SeatsPage() {
  const navigate = useNavigate();
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
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
      const message = err?.response?.data?.error || err?.message || "Failed to load seats.";
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
      const message = err?.response?.data?.error || err?.message || "Failed to load reservations.";
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

  const handleSeatSelect = async (seat) => {
    const accepted = window.confirm(`Do you want to book seat ${seat.seatNumber}?`);
    if (!accepted) {
      return;
    }

    setError("");
    setBookingSeatId(seat.id);

    try {
      const reservation = await reserveSeat(seat.id);
      await loadSeats();
      await loadReservations();
      navigate(`/payment/${reservation.id}`, {
        state: { reservation },
      });
    } catch (err) {
      const message = err?.response?.status === 409
        ? "Seat is no longer available."
        : (err?.response?.data?.error || err?.message || "Failed to reserve seat.");
      setError(message);
      await loadSeats();
      await loadReservations();
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
              />
            )}
          </div>
        </section>
      </section>
    </main>
  );
}

export default SeatsPage;

