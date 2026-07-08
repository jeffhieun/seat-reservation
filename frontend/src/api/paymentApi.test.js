import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "../services/axiosClient";
import {
  completePayment,
  completePaymentForTesting,
  getPaymentById,
  initiatePayment,
} from "./paymentApi";

vi.mock("../services/axiosClient", () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

describe("paymentApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("initiates payment via POST /api/payments", async () => {
    axiosClient.post.mockResolvedValue({ data: { id: 10, status: "PENDING" } });

    const result = await initiatePayment(99);

    expect(axiosClient.post).toHaveBeenCalledWith("/api/payments", null, {
      params: { reservationId: 99 },
    });
    expect(result).toEqual({ id: 10, status: "PENDING" });
  });

  it("fetches payment by id", async () => {
    axiosClient.get.mockResolvedValue({ data: { id: 11, status: "SUCCESS" } });

    const result = await getPaymentById(11);

    expect(axiosClient.get).toHaveBeenCalledWith("/api/payments/11");
    expect(result).toEqual({ id: 11, status: "SUCCESS" });
  });

  it("completes payment via POST /api/payments/{id}", async () => {
    axiosClient.post.mockResolvedValue({ data: { paymentId: 11, status: "SUCCESS" } });

    const result = await completePayment(11, "SUCCESS");

    expect(axiosClient.post).toHaveBeenCalledWith("/api/payments/11", {
      result: "SUCCESS",
    });
    expect(result).toEqual({ paymentId: 11, status: "SUCCESS" });
  });

  it("completes payment via testing endpoint", async () => {
    axiosClient.post.mockResolvedValue({ data: { paymentId: 11, status: "FAILED" } });

    const result = await completePaymentForTesting(11, "FAILED");

    expect(axiosClient.post).toHaveBeenCalledWith("/api/payments/11/complete", {
      result: "FAILED",
    });
    expect(result).toEqual({ paymentId: 11, status: "FAILED" });
  });
});
