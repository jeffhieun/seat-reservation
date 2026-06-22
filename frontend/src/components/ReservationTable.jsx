import "./ReservationTable.css";

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

function ReservationTable({ reservations, activeTab, onPay }) {
  if (!reservations || reservations.length === 0) {
    return <p className="reservation-table-empty">No reservations found</p>;
  }

  return (
    <div className="reservation-table-wrap">
      <table className="reservation-table">
        <colgroup>
          <col className="reservation-col-id" />
          <col className="reservation-col-seat" />
          <col className="reservation-col-status" />
          <col className="reservation-col-created" />
          <col className="reservation-col-action" />
        </colgroup>
        <thead>
          <tr>
            <th>Reservation ID</th>
            <th>Seat Number</th>
            <th>Status</th>
            <th>Created At</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {reservations.map((reservation) => (
            <tr key={reservation.id}>
              <td>{reservation.id}</td>
              <td>{getSeatNumber(reservation)}</td>
              <td>{reservation.status || "-"}</td>
              <td>{formatDate(reservation.createdAt || reservation.created_at)}</td>
              <td>
                {activeTab === "PENDING_PAYMENT" ? (
                  <button
                    type="button"
                    className="reservation-pay-btn"
                    onClick={() => onPay(reservation)}
                  >
                    Pay Now
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

