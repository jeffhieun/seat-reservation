import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PaymentPage from "./PaymentPage";
import { getReservationById } from "../api/reservationApi";
import { completePayment, getPaymentById, initiatePayment } from "../api/paymentApi";

const navigateMock = vi.fn();

vi.mock("../api/reservationApi", () => ({
  getReservationById: vi.fn(),
}));

vi.mock("../api/paymentApi", () => ({
  initiatePayment: vi.fn(),
  getPaymentById: vi.fn(),
  completePayment: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
    useParams: () => ({ reservationId: "5" }),
    useLocation: () => ({
      state: {
        reservation: { id: 5, seat_number: "A1", status: "PENDING_PAYMENT" },
      },
      search: "",
    }),
  };
});

vi.mock("../components/Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

describe("PaymentPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getReservationById.mockResolvedValue({ id: 5, seat_number: "A1", status: "CONFIRMED" });
    initiatePayment.mockResolvedValue({
      id: 20,
      reservation_id: 5,
      status: "PENDING",
      amount: "10.00",
    });
    getPaymentById.mockResolvedValue({
      id: 20,
      reservation_id: 5,
      status: "PENDING",
      amount: "10.00",
    });
  });

  it("shows loading state and disables complete button while request is running", async () => {
    let resolver;
    completePayment.mockImplementation(() => new Promise((resolve) => {
      resolver = resolve;
    }));

    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.queryByText("Loading payment...")).not.toBeInTheDocument();
    });
    const button = await screen.findByRole("button", { name: "Complete Payment" });
    fireEvent.click(button);

    expect(await screen.findByRole("button", { name: "Completing Payment..." })).toBeDisabled();

    resolver({ paymentId: 20, status: "SUCCESS", reservationStatus: "CONFIRMED" });
    await waitFor(() => {
      expect(completePayment).toHaveBeenCalledWith(20, "SUCCESS");
    });
  });

  it("navigates to success only after backend confirms payment", async () => {
    completePayment.mockResolvedValue({ paymentId: 20, status: "SUCCESS", reservationStatus: "CONFIRMED" });
    getPaymentById
      .mockResolvedValueOnce({ id: 20, reservation_id: 5, status: "PENDING", amount: "10.00" })
      .mockResolvedValueOnce({ id: 20, reservation_id: 5, status: "SUCCESS", amount: "10.00" });
    getReservationById.mockResolvedValue({ id: 5, seat_number: "A1", status: "CONFIRMED" });

    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.queryByText("Loading payment...")).not.toBeInTheDocument();
    });
    const button = await screen.findByRole("button", { name: "Complete Payment" });
    fireEvent.click(button);

    await waitFor(() => {
      expect(completePayment).toHaveBeenCalledWith(20, "SUCCESS");
      expect(navigateMock).toHaveBeenCalledWith("/success", expect.any(Object));
    });
  });

  it("keeps user on payment page and shows error when completion fails", async () => {
    completePayment.mockRejectedValue({
      response: {
        data: { message: "Payment failed at gateway." },
      },
    });

    render(<PaymentPage />);

    await waitFor(() => {
      expect(screen.queryByText("Loading payment...")).not.toBeInTheDocument();
    });
    const button = await screen.findByRole("button", { name: "Complete Payment" });
    fireEvent.click(button);

    expect(await screen.findByText("Payment failed at gateway.")).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalledWith("/success", expect.anything());
  });
});
