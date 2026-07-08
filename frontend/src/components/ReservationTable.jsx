import "./ReservationTable.css";
import useCountdown from "../hooks/useCountdown";

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

function getSeatNumber(reservation) {
  return reservation?.seatNumber || reservation?.seat_number || "-";
}

function getStatusClass(status) {
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

function getExpiresAt(reservation) {
  return reservation?.expiresAt || reservation?.expires_at || null;
}

function RemainingTimeCell({ reservation }) {
  const status = reservation?.status;
  const { label } = useCountdown(getExpiresAt(reservation));

  if (status !== "PENDING_PAYMENT") {
    return <span>-</span>;
  }

  return <span>{label}</span>;
}

function ReservationTable({
  reservations,
  activeTab,
  onPay,
  onViewDetails,
  paymentInProgressReservationId = null,
}) {
  if (!reservations || reservations.length === 0) {
    return <p className="reservation-table-empty">No reservations found.</p>;
  }

  return (
    <div className="reservation-table-wrap">
      <table className="reservation-table">
        <colgroup>
          <col className="reservation-col-id" />
          <col className="reservation-col-seat" />
          <col className="reservation-col-status" />
          <col className="reservation-col-remaining" />
          <col className="reservation-col-created" />
          <col className="reservation-col-action" />
        </colgroup>
        <thead>
          <tr>
            <th>Reservation ID</th>
            <th>Seat Number</th>
            <th>Status</th>
            <th>Remaining Time</th>
            <th>Created At</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {reservations.map((reservation) => (
            <tr key={reservation.id}>
              <td>{reservation.id}</td>
              <td>{getSeatNumber(reservation)}</td>
              <td>
                <span className={`reservation-badge ${getStatusClass(reservation.status)}`}>
                  {reservation.status || "-"}
                </span>
              </td>
              <td>
                <RemainingTimeCell reservation={reservation} />
              </td>
              <td>{formatDate(reservation.createdAt || reservation.created_at)}</td>
              <td>
                <button
                  type="button"
                  className="reservation-view-btn"
                  onClick={() => onViewDetails(reservation)}
                >
                  View
                </button>
                {activeTab === "PENDING_PAYMENT" ? (
                  <button
                    type="button"
                    className="reservation-pay-btn"
                    onClick={() => onPay(reservation)}
                    disabled={paymentInProgressReservationId === reservation.id}
                  >
                    {paymentInProgressReservationId === reservation.id ? "Processing..." : "Pay Now"}
                  </button>
                ) : null}
                {activeTab === "CONFIRMED" ? (
                  <span>{formatDate(reservation.confirmedAt || reservation.confirmed_at)}</span>
                ) : null}
                {activeTab === "EXPIRED" ? (
                  <span>{formatDate(reservation.expiredAt || reservation.expired_at)}</span>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ReservationTable;
