import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import PaymentFailedPage from "./PaymentFailedPage";

const navigateMock = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
    useLocation: () => ({
      state: {
        reservationId: 42,
      },
    }),
  };
});

vi.mock("../components/Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

describe("PaymentFailedPage", () => {
  it("renders failed state and supports retry/back actions", () => {
    render(<PaymentFailedPage />);

    expect(screen.getByRole("heading", { name: "Payment Failed" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(navigateMock).toHaveBeenCalledWith("/payment/42", { replace: true });

    fireEvent.click(screen.getByRole("button", { name: "Back to Reservations" }));
    expect(navigateMock).toHaveBeenCalledWith("/seats");
  });
});
