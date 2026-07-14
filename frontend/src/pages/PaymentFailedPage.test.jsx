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
        payment: {
          id: 20,
          failureReason: "Card declined",
        },
      },
    }),
  };
});

vi.mock("../components/Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

describe("PaymentFailedPage", () => {
  it("renders failed state with failure reason and supports retry/back actions", () => {
    render(<PaymentFailedPage />);

    expect(screen.getByRole("heading", { name: "Payment Failed" })).toBeInTheDocument();
    expect(screen.getByText("Card declined")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Retry payment/ }));
    expect(navigateMock).toHaveBeenCalledWith("/payment/42", { replace: true });

    fireEvent.click(screen.getByRole("button", { name: /Return to seat selection/ }));
    expect(navigateMock).toHaveBeenCalledWith("/seats");
  });

  it("renders back to seats button", () => {
    render(<PaymentFailedPage />);

    expect(screen.getByRole("button", { name: /Return to seats page/ })).toBeInTheDocument();
  });
});

