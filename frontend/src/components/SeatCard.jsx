import "./SeatCard.css";

const STATUS_CLASS_MAP = {
  AVAILABLE: "seat-card-available",
  RESERVED: "seat-card-reserved",
  BOOKED: "seat-card-booked",
  MAINTENANCE: "seat-card-maintenance",
};

function SeatCard({ seat, onSelect, disabled = false }) {
  const status = seat?.status || "UNKNOWN";
  const isAvailable = status === "AVAILABLE";
  const isSelectable = isAvailable && !disabled;
  const className = STATUS_CLASS_MAP[status] || "seat-maintenance";

  const handleClick = () => {
    if (isSelectable) {
      onSelect(seat);
    }
  };

  return (
    <button
      type="button"
      className={`seat-card ${className}`}
      onClick={handleClick}
      disabled={!isSelectable}
    >
      <div className="seat-card-number">{seat?.seatNumber || "-"}</div>
      <div className="seat-card-status">{status}</div>
    </button>
  );
}

export default SeatCard;
