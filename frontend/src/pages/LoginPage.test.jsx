import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import LoginPage from "./LoginPage";
import { login } from "../api/authApi";

const navigateMock = vi.fn();
const loadCurrentUserMock = vi.fn();

vi.mock("../api/authApi", () => ({
  login: vi.fn(),
}));

vi.mock("../context/AuthContext", () => ({
  useAuth: () => ({
    loadCurrentUser: loadCurrentUserMock,
  }),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

describe("LoginPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  it("logs in successfully and navigates to seats", async () => {
    login.mockResolvedValue({
      token: "token-1",
      email: "user@test.com",
    });
    loadCurrentUserMock.mockResolvedValue({});

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "user@test.com" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } });
    fireEvent.click(screen.getByRole("button", { name: "Login" }));

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith("user@test.com", "password123");
      expect(loadCurrentUserMock).toHaveBeenCalled();
      expect(navigateMock).toHaveBeenCalledWith("/seats", { replace: true });
    });

    expect(window.localStorage.getItem("token")).toBe("token-1");
    expect(window.localStorage.getItem("userEmail")).toBe("user@test.com");
  });

  it("shows login failure message", async () => {
    login.mockRejectedValue({
      response: {
        data: {
          message: "Invalid credentials",
        },
      },
    });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "user@test.com" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "wrong-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Login" }));

    expect(await screen.findByText("Invalid credentials")).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalledWith("/seats", { replace: true });
  });
});
