import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PaymentPage from "./PaymentPage";
import { getReservationById } from "../api/reservationApi";
import { initiatePayment } from "../api/paymentApi";

const navigateMock = vi.fn();

vi.mock("../api/reservationApi", () => ({
  getReservationById: vi.fn(),
}));

vi.mock("../api/paymentApi", () => ({
  initiatePayment: vi.fn(),
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
    window.sessionStorage.clear();
    getReservationById.mockResolvedValue({ id: 5, seat_number: "A1", status: "PENDING_PAYMENT" });
    initiatePayment.mockResolvedValue({
      id: 20,
      reservationId: 5,
      status: "PENDING",
      amount: "10.00",
    });
  });

  it("initiates payment and navigates to processing page", async () => {
    render(<PaymentPage />);

    fireEvent.click(screen.getByRole("button", { name: "Pay" }));

    await waitFor(() => {
      expect(initiatePayment).toHaveBeenCalledWith("5");
      expect(navigateMock).toHaveBeenCalledWith("/payment/processing/20", expect.any(Object));
    });
  });

  it("disables pay button while request is in progress", async () => {
    let resolver;
    initiatePayment.mockImplementation(() => new Promise((resolve) => {
      resolver = resolve;
    }));

    render(<PaymentPage />);
    fireEvent.click(screen.getByRole("button", { name: "Pay" }));

    expect(screen.getByRole("button", { name: "Processing..." })).toBeDisabled();

    resolver({ id: 20, reservationId: 5, status: "PENDING" });
  });

  it("back to seats works without initiating payment", async () => {
    render(<PaymentPage />);
    fireEvent.click(screen.getByRole("button", { name: "← Back to Seats" }));

    expect(navigateMock).toHaveBeenCalledWith("/seats");
    expect(initiatePayment).not.toHaveBeenCalled();
  });
});
