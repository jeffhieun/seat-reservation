import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "../services/axiosClient";
import { getCurrentUser, login, register } from "./authApi";

vi.mock("../services/axiosClient", () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

describe("authApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("gets current user via GET /api/auth/me", async () => {
    axiosClient.get.mockResolvedValue({
      data: {
        id: 1,
        email: "user@example.com",
        createdAt: "2026-01-01T00:00:00",
        updatedAt: "2026-01-01T00:00:00",
      },
    });

    const result = await getCurrentUser();

    expect(axiosClient.get).toHaveBeenCalledWith("/api/auth/me");
    expect(result.email).toBe("user@example.com");
  });

  it("logs in via POST /api/auth/login", async () => {
    axiosClient.post.mockResolvedValue({
      data: { token: "token", refreshToken: "refresh", email: "user@example.com" },
    });

    const result = await login("user@example.com", "password123");

    expect(axiosClient.post).toHaveBeenCalledWith("/api/auth/login", {
      email: "user@example.com",
      password: "password123",
    });
    expect(result.token).toBe("token");
  });

  it("registers via POST /api/auth/register", async () => {
    axiosClient.post.mockResolvedValue({ data: {} });

    await register("user@example.com", "password123");

    expect(axiosClient.post).toHaveBeenCalledWith("/api/auth/register", {
      email: "user@example.com",
      password: "password123",
    });
  });
});
