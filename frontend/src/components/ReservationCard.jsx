import "./ReservationCard.css";

function formatDate(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleString();
}

function getBadgeClass(status) {
  if (status === "PENDING_PAYMENT") {
    return "reservation-badge-pending";
  }
  if (status === "CONFIRMED") {
    return "reservation-badge-confirmed";
  }
  if (status === "EXPIRED") {
    return "reservation-badge-expired";
  }
  return "reservation-badge-unknown";
}

function ReservationCard({ reservation, loading, error, onConfirm, onExpire, actionLoading }) {
  if (loading) {
    return <p className="empty-state">Loading reservation details...</p>;
  }

  if (error) {
    return <p className="error-text">{error}</p>;
  }

  if (!reservation) {
    return <p className="empty-state">Reservation not found.</p>;
  }

  const status = reservation.status || "-";
  const isPending = status === "PENDING_PAYMENT";

  return (
    <section className="card reservation-card">
      <h3>Reservation Details</h3>
      <p><strong>Reservation ID:</strong> {reservation.id}</p>
      <p><strong>Seat:</strong> {reservation.seat_number || reservation.seatNumber || "-"}</p>
      <p>
        <strong>Status:</strong>
        {" "}
        <span className={`reservation-badge ${getBadgeClass(status)}`}>{status}</span>
      </p>
      <p><strong>Created Time:</strong> {formatDate(reservation.created_at || reservation.createdAt)}</p>
      <p><strong>Confirmed Time:</strong> {formatDate(reservation.confirmed_at || reservation.confirmedAt)}</p>
      <p><strong>Expiration Time:</strong> {formatDate(reservation.expired_at || reservation.expiredAt)}</p>

      <div className="reservation-card-actions">
        <button
          type="button"
          className="btn"
          onClick={onConfirm}
          disabled={!isPending || actionLoading}
        >
          {actionLoading ? "Updating..." : "Confirm"}
        </button>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={onExpire}
          disabled={!isPending || actionLoading}
        >
          {actionLoading ? "Updating..." : "Expire"}
        </button>
      </div>
    </section>
  );
}

export default ReservationCard;

