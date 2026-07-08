import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ReservationTable from "./ReservationTable";

describe("ReservationTable", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("renders empty state", () => {
    render(
      <ReservationTable
        reservations={[]}
        activeTab="PENDING_PAYMENT"
        onPay={vi.fn()}
        onViewDetails={vi.fn()}
      />
    );

    expect(screen.getByText("No reservations found.")).toBeInTheDocument();
  });

  it("calls view and pay actions for pending reservation", () => {
    const onPay = vi.fn();
    const onViewDetails = vi.fn();
    const reservation = {
      id: 1,
      seat_number: "A01",
      status: "PENDING_PAYMENT",
      created_at: "2026-01-01T10:00:00",
    };

    render(
      <ReservationTable
        reservations={[reservation]}
        activeTab="PENDING_PAYMENT"
        onPay={onPay}
        onViewDetails={onViewDetails}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: "View" }));
    fireEvent.click(screen.getByRole("button", { name: "Pay Now" }));

    expect(onViewDetails).toHaveBeenCalledWith(reservation);
    expect(onPay).toHaveBeenCalledWith(reservation);
  });

  it("renders countdown and shows Expired when time runs out", async () => {
    vi.setSystemTime(new Date("2026-01-01T10:00:00.000Z"));
    const reservation = {
      id: 10,
      seat_number: "A10",
      status: "PENDING_PAYMENT",
      created_at: "2026-01-01T09:59:00.000Z",
      expires_at: "2026-01-01T10:00:05.000Z",
    };

    render(
      <ReservationTable
        reservations={[reservation]}
        activeTab="PENDING_PAYMENT"
        onPay={vi.fn()}
        onViewDetails={vi.fn()}
      />
    );

    expect(screen.getByText("00:05")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(6000);
    });

    expect(screen.getByText("Expired")).toBeInTheDocument();
  });
});
