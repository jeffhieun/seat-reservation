import SeatCard from "./SeatCard";
import "./SeatGrid.css";

function SeatGrid({ seats, onSeatSelect, disabled = false }) {
  if (!seats || seats.length === 0) {
    return <p className="seat-grid-empty">No seats found</p>;
  }

  return (
    <section className="seat-grid" aria-label="Seat list">
      {seats.map((seat) => (
        <SeatCard key={seat.id} seat={seat} onSelect={onSeatSelect} disabled={disabled} />
      ))}
    </section>
  );
}

export default SeatGrid;
