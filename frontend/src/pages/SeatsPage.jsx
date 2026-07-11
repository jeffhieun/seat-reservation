import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getSeats } from "../api/seatApi";
import { getUserReservations, reserveSeat } from "../api/reservationApi";
import Navbar from "../components/Navbar";
import SeatGrid from "../components/SeatGrid";
import ReservationTabs from "../components/ReservationTabs";
import ReservationTable from "../components/ReservationTable";
import LoadingSpinner from "../components/common/LoadingSpinner";
import useReservationPolling from "../hooks/useReservationPolling";

const AUTO_REFRESH_MS = 5000;
const ACTIVE_RESERVATION_CONFLICT_MESSAGE = "You already have an active reservation for this seat.";
const SEAT_RESERVED_BY_OTHER_USER_MESSAGE = "Seat has already been reserved by another user.";

function extractErrorMessage(err, fallbackMessage) {
  return err?.response?.data?.message
    || err?.response?.data?.error
    || err?.message
    || fallbackMessage;
}

function getReservationConflictMessage(err) {
  const conflictMessage = err?.response?.data?.message;

  if (
    conflictMessage === ACTIVE_RESERVATION_CONFLICT_MESSAGE
    || conflictMessage === "You already reserved this seat."
  ) {
    return ACTIVE_RESERVATION_CONFLICT_MESSAGE;
  }

  return SEAT_RESERVED_BY_OTHER_USER_MESSAGE;
}

function areCollectionsEqual(previousData, nextData) {
  return JSON.stringify(previousData) === JSON.stringify(nextData);
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
  const [paymentInProgressReservationId, setPaymentInProgressReservationId] = useState(null);

  const loadSeats = useCallback(async ({ silent = false } = {}) => {
    if (!silent) {
      setLoading(true);
      setError("");
    }

    try {
      const data = await getSeats();
      const nextSeats = Array.isArray(data) ? data : [];
      setSeats((previousSeats) => (areCollectionsEqual(previousSeats, nextSeats) ? previousSeats : nextSeats));
    } catch (err) {
      if (!silent) {
        const message = extractErrorMessage(err, "Failed to load seats.");
        setError(message);
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, []);

  const loadReservations = useCallback(async ({ silent = false } = {}) => {
    if (!silent) {
      setReservationsLoading(true);
    }

    try {
      const data = await getUserReservations();
      const nextReservations = Array.isArray(data) ? data : [];
      setReservations((previousReservations) => (
        areCollectionsEqual(previousReservations, nextReservations) ? previousReservations : nextReservations
      ));
    } catch (err) {
      if (!silent) {
        const message = extractErrorMessage(err, "Failed to load reservations.");
        setError(message);
        setReservations([]);
      }
    } finally {
      if (!silent) {
        setReservationsLoading(false);
      }
    }
  }, []);

  const refreshAll = useCallback(
    ({ silent = false } = {}) => Promise.all([
      loadSeats({ silent }),
      loadReservations({ silent }),
    ]),
    [loadReservations, loadSeats]
  );

  useEffect(() => {
    refreshAll();
  }, [refreshAll]);

  const { isPolling } = useReservationPolling({
    enabled: true,
    intervalMs: AUTO_REFRESH_MS,
    pollFn: () => refreshAll({ silent: true }),
  });

  useEffect(() => {
    if (location.state?.shouldRefreshReservations) {
      refreshAll();
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, location.state, navigate, refreshAll]);

  const handleSeatSelect = async (seat) => {
    if (bookingSeatId !== null) {
      return;
    }

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
      await refreshAll({ silent: true });
      navigate(`/payment/${reservation.id}`, {
        state: { reservation },
      });
    } catch (err) {
      const isConflict = err?.response?.status === 409;
      const message = isConflict
        ? getReservationConflictMessage(err)
        : extractErrorMessage(err, "Failed to reserve seat.");
      setError(message);
      if (!isConflict) {
        await refreshAll({ silent: true });
      }
    } finally {
      setBookingSeatId(null);
    }
  };

  const handlePayReservation = (reservation) => {
    if (!reservation?.id) {
      return;
    }
    setPaymentInProgressReservationId(reservation.id);

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
          </div>

          {successMessage ? <p className="success-text">{successMessage}</p> : null}
          {error ? <p className="error-text">{error}</p> : null}
          {!loading && isPolling ? <p className="info-text">Refreshing data...</p> : null}

          {bookingSeatId !== null ? <p className="info-text">Booking seat...</p> : null}

          <div className="seats-grid-wrap">
            {loading ? (
              <LoadingSpinner message="Loading seats..." />
            ) : (
              <SeatGrid
                seats={seats}
                onSeatSelect={handleSeatSelect}
                disabled={bookingSeatId !== null}
              />
            )}
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
              <LoadingSpinner message="Loading reservations..." />
            ) : (
              <ReservationTable
                reservations={filteredReservations}
                activeTab={activeTab}
                onPay={handlePayReservation}
                onViewDetails={handleViewReservation}
                paymentInProgressReservationId={paymentInProgressReservationId}
              />
            )}
          </div>
        </section>
      </section>
    </main>
  );
}

export default SeatsPage;
