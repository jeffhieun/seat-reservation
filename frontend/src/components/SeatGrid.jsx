import SeatCard from "./SeatCard";
import "./SeatGrid.css";

function SeatGrid({ seats, onSeatSelect }) {
  if (!seats || seats.length === 0) {
    return <p className="seat-grid-empty">No seats found</p>;
  }

  return (
    <section className="seat-grid" aria-label="Seat list">
      {seats.map((seat) => (
        <SeatCard key={seat.id} seat={seat} onSelect={onSeatSelect} />
      ))}
    </section>
  );
}

export default SeatGrid;

