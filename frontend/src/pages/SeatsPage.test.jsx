import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SeatsPage from "./SeatsPage";
import { getSeats } from "../api/seatApi";
import { getUserReservations, reserveSeat } from "../api/reservationApi";

const navigateMock = vi.fn();

vi.mock("../api/seatApi", () => ({
  getSeats: vi.fn(),
}));

vi.mock("../api/reservationApi", () => ({
  getUserReservations: vi.fn(),
  reserveSeat: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
    useLocation: () => ({ pathname: "/seats", state: null }),
  };
});

vi.mock("../components/Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

vi.mock("../components/ReservationTabs", () => ({
  default: ({ pendingCount, confirmedCount, expiredCount }) => (
    <div>
      Tabs {pendingCount}/{confirmedCount}/{expiredCount}
    </div>
  ),
}));

vi.mock("../components/ReservationTable", () => ({
  default: ({ reservations }) => (
    <div>
      Table {reservations.map((reservation) => reservation.status).join(",")}
    </div>
  ),
}));

vi.mock("../components/SeatGrid", () => ({
  default: ({ seats, onSeatSelect }) => (
    <div>
      {seats.map((seat) => (
        <button key={seat.id} type="button" onClick={() => onSeatSelect(seat)}>
          Reserve {seat.seatNumber}
        </button>
      ))}
    </div>
  ),
}));

describe("SeatsPage duplicate reservation handling", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    getSeats.mockResolvedValue([{ id: 1, seatNumber: "A1", status: "AVAILABLE" }]);
    getUserReservations.mockResolvedValue([]);
  });

  it("handles 409 conflict without navigation and clears loading state", async () => {
    reserveSeat.mockRejectedValue({
      response: {
        status: 409,
        data: {
          message: "You already have an active reservation for this seat.",
        },
      },
    });

    render(<SeatsPage />);

    const reserveButton = await screen.findByRole("button", { name: "Reserve A1" });
    fireEvent.click(reserveButton);

    await screen.findByText("You already have an active reservation for this seat.");
    await waitFor(() => {
      expect(screen.queryByText("Booking seat...")).not.toBeInTheDocument();
    });

    expect(navigateMock).not.toHaveBeenCalled();
    expect(reserveSeat).toHaveBeenCalledWith(1);
    expect(getSeats).toHaveBeenCalledTimes(1);
    expect(getUserReservations).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Reserve A1" })).toBeInTheDocument();
  });

  it("shows seat conflict message when another user already reserved the seat", async () => {
    reserveSeat.mockRejectedValue({
      response: {
        status: 409,
        data: {},
      },
    });

    render(<SeatsPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Reserve A1" }));

    await screen.findByText("Seat has already been reserved by another user.");
    expect(navigateMock).not.toHaveBeenCalled();
    expect(screen.queryByText("Booking seat...")).not.toBeInTheDocument();
  });

  it("polls seats and reservations every 5 seconds", async () => {
    vi.useFakeTimers();
    render(<SeatsPage />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(getSeats).toHaveBeenCalledTimes(1);
    expect(getUserReservations).toHaveBeenCalledTimes(1);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(getSeats).toHaveBeenCalledTimes(2);
    expect(getUserReservations).toHaveBeenCalledTimes(2);
    vi.useRealTimers();
  });

  it("updates reservation status counts after polling", async () => {
    vi.useFakeTimers();
    getUserReservations
      .mockResolvedValueOnce([{ id: 1, status: "PENDING_PAYMENT", seat_number: "A1" }])
      .mockResolvedValueOnce([{ id: 1, status: "EXPIRED", seat_number: "A1" }]);

    render(<SeatsPage />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText("Tabs 1/0/0")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });

    expect(screen.getByText("Tabs 0/0/1")).toBeInTheDocument();
    vi.useRealTimers();
  });
});
