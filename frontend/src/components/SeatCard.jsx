const STATUS_CLASS_MAP = {
  AVAILABLE: "seat-available",
  RESERVED: "seat-reserved",
  BOOKED: "seat-booked",
  MAINTENANCE: "seat-maintenance",
};

function SeatCard({ seat, onSelect }) {
  const status = seat?.status || "UNKNOWN";
  const isAvailable = status === "AVAILABLE";
  const className = STATUS_CLASS_MAP[status] || "seat-maintenance";

  const handleClick = () => {
    if (isAvailable) {
      onSelect(seat);
    }
  };

  return (
    <button
      type="button"
      className={`seat-card ${className}`}
      onClick={handleClick}
      disabled={!isAvailable}
    >
      <div className="seat-number">{seat?.seatNumber || "-"}</div>
      <div className="seat-status">{status}</div>
    </button>
  );
}

export default SeatCard;

