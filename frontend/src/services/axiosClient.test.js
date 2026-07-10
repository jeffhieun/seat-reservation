import { beforeEach, describe, expect, it, vi } from "vitest";

const requestUse = vi.fn();
const responseUse = vi.fn();
const axiosCreate = vi.fn(() => ({
  interceptors: {
    request: { use: requestUse },
    response: { use: responseUse },
  },
}));
const axiosPost = vi.fn();

vi.mock("axios", () => ({
  default: {
    create: axiosCreate,
    post: axiosPost,
  },
}));

const { default: axiosClient } = await import("./axiosClient");

describe("axiosClient", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("configures JSON content type and Authorization header", () => {
    expect(axiosCreate).toHaveBeenCalledWith({
      baseURL: "http://localhost:8080",
      timeout: 10000,
      headers: {
        "Content-Type": "application/json",
      },
    });

    localStorage.setItem("token", "abc123");
    const requestHandler = requestUse.mock.calls[0][0];
    const config = requestHandler({ headers: {} });

    expect(config.headers.Authorization).toBe("Bearer abc123");
  });
});
