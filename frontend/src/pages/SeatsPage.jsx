import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getSeats } from "../api/seatApi";
import { reserveSeat } from "../api/reservationApi";
import Navbar from "../components/Navbar";
import SeatGrid from "../components/SeatGrid";

function SeatsPage() {
  const navigate = useNavigate();
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [bookingSeatId, setBookingSeatId] = useState(null);

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

  useEffect(() => {
    loadSeats();
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
      navigate(`/payment/${reservation.id}`, {
        state: { reservation },
      });
    } catch (err) {
      const message = err?.response?.status === 409
        ? "Seat is no longer available."
        : (err?.response?.data?.error || err?.message || "Failed to reserve seat.");
      setError(message);
      await loadSeats();
    } finally {
      setBookingSeatId(null);
    }
  };

  return (
    <main>
      <Navbar />
      <section className="page-content">
        <div className="page-header">
          <h2>Seats</h2>
          <button className="btn" type="button" onClick={loadSeats} disabled={loading || bookingSeatId !== null}>
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        </div>

        {error ? <p className="error-text">{error}</p> : null}

        {bookingSeatId !== null ? <p className="info-text">Booking seat...</p> : null}

        {loading ? <p>Loading seats...</p> : <SeatGrid seats={seats} onSeatSelect={handleSeatSelect} />}
      </section>
    </main>
  );
}

export default SeatsPage;

