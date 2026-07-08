import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PaymentProcessingPage from "./PaymentProcessingPage";
import { getPaymentById } from "../api/paymentApi";
import { getReservationById } from "../api/reservationApi";

const navigateMock = vi.fn();

vi.mock("../api/paymentApi", () => ({
  getPaymentById: vi.fn(),
}));

vi.mock("../api/reservationApi", () => ({
  getReservationById: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
    useParams: () => ({ paymentId: "20" }),
    useLocation: () => ({ state: { reservationId: 5 } }),
  };
});

vi.mock("../components/Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

describe("PaymentProcessingPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getReservationById.mockResolvedValue({ id: 5, seat_number: "A1", status: "CONFIRMED" });
  });

  it("polls and navigates to success when backend confirms", async () => {
    getPaymentById.mockResolvedValue({
      id: 20,
      status: "SUCCESS",
      reservationId: 5,
      reservationStatus: "CONFIRMED",
      providerReference: "PAY_REF",
    });

    render(<PaymentProcessingPage />);

    await waitFor(() => {
      expect(getPaymentById).toHaveBeenCalledWith("20");
      expect(navigateMock).toHaveBeenCalledWith("/payment/success", expect.any(Object));
    });
  });

  it("navigates to failed page when backend returns FAILED", async () => {
    getPaymentById.mockResolvedValue({
      id: 20,
      status: "FAILED",
      reservationId: 5,
      reservationStatus: "PENDING_PAYMENT",
    });

    render(<PaymentProcessingPage />);

    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith("/payment/failed", expect.any(Object));
    });
  });

  it("shows timeout after 60 seconds and allows retry", async () => {
    vi.useFakeTimers();
    getPaymentById.mockResolvedValue({
      id: 20,
      status: "PENDING",
      reservationId: 5,
      reservationStatus: "PENDING_PAYMENT",
    });

    render(<PaymentProcessingPage />);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(60000);
    });

    expect(screen.getByText("Payment timed out. Please retry.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(navigateMock).toHaveBeenCalledWith("/payment/5", expect.any(Object));
    vi.useRealTimers();
  });
});
