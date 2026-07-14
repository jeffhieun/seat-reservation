import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SuccessPage from "./SuccessPage";

const navigateMock = vi.fn();
let useLocationMock = () => ({
  state: {
    reservation: {
      id: 1,
      seat_number: "A1",
      status: "CONFIRMED",
    },
    payment: {
      id: 10,
      status: "SUCCESS",
      amount: "10.00",
      providerReference: "PAY_ABC123DEF456",
      createdAt: "2024-07-13T10:00:00",
    },
  },
});

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
    useLocation: () => useLocationMock(),
  };
});

vi.mock("../components/Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

describe("SuccessPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useLocationMock = () => ({
      state: {
        reservation: {
          id: 1,
          seat_number: "A1",
          status: "CONFIRMED",
        },
        payment: {
          id: 10,
          status: "SUCCESS",
          amount: "10.00",
          providerReference: "PAY_ABC123DEF456",
          createdAt: "2024-07-13T10:00:00",
        },
      },
    });
  });

  it("displays reservation and payment confirmation details", () => {
    render(<SuccessPage />);

    expect(screen.getByRole("heading", { name: "Booking Successful" })).toBeInTheDocument();
    expect(screen.getByText(/Reservation ID:/)).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText(/Seat Number:/)).toBeInTheDocument();
    expect(screen.getByText("A1")).toBeInTheDocument();
    expect(screen.getByText(/Reservation Status:/)).toBeInTheDocument();
    expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    expect(screen.getByText(/Payment ID:/)).toBeInTheDocument();
    expect(screen.getByText("10")).toBeInTheDocument();
    expect(screen.getByText(/Payment Status:/)).toBeInTheDocument();
    expect(screen.getByText("SUCCESS")).toBeInTheDocument();
    expect(screen.getByText(/Amount:/)).toBeInTheDocument();
    expect(screen.getByText("10.00")).toBeInTheDocument();
    expect(screen.getByText(/Provider Reference:/)).toBeInTheDocument();
    expect(screen.getByText("PAY_ABC123DEF456")).toBeInTheDocument();
    expect(screen.getByText(/Confirmation Time:/)).toBeInTheDocument();
  });

  it("navigates to seats when back button is clicked", () => {
    render(<SuccessPage />);

    fireEvent.click(screen.getByRole("button", { name: "Back to Seats" }));

    expect(navigateMock).toHaveBeenCalledWith("/seats", { replace: true });
  });

  it("shows empty state when no reservation data is available", () => {
    useLocationMock = () => ({ state: {} });

    render(<SuccessPage />);

    expect(screen.getByText("No booking data available.")).toBeInTheDocument();
  });
});


