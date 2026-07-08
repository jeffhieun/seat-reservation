import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import RegisterPage from "./RegisterPage";
import { register } from "../api/authApi";

const navigateMock = vi.fn();

vi.mock("../api/authApi", () => ({
  register: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

describe("RegisterPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows validation error when confirm password does not match", async () => {
    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "user@example.com" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Confirm Password"), { target: { value: "password999" } });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Confirm password must match password.")).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it("submits registration and redirects to login", async () => {
    vi.useFakeTimers();
    register.mockResolvedValue({});

    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "user@example.com" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Confirm Password"), { target: { value: "password123" } });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await act(async () => {
      await Promise.resolve();
    });
    expect(register).toHaveBeenCalledWith("user@example.com", "password123");
    expect(screen.getByText("Registration successful. Redirecting to login...")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1200);
    });

    expect(navigateMock).toHaveBeenCalledWith("/login", { replace: true });
    vi.useRealTimers();
  });
});
