import { describe, expect, it, vi, beforeEach } from "vitest";
import axiosClient from "../services/axiosClient";
import {
  confirmReservation,
  expireReservation,
  getReservationById,
  getUserReservations,
  reserveSeat,
} from "./reservationApi";

vi.mock("../services/axiosClient", () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

describe("reservationApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("reserveSeat should call POST /api/reservations", async () => {
    axiosClient.post.mockResolvedValue({ data: { id: 1 } });

    const result = await reserveSeat(9);

    expect(axiosClient.post).toHaveBeenCalledWith("/api/reservations", { seatId: 9 });
    expect(result).toEqual({ id: 1 });
  });

  it("should fetch reservation list and detail", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: [{ id: 1 }] });
    axiosClient.get.mockResolvedValueOnce({ data: { id: 1 } });

    const list = await getUserReservations();
    const detail = await getReservationById(1);

    expect(axiosClient.get).toHaveBeenNthCalledWith(1, "/api/reservations");
    expect(axiosClient.get).toHaveBeenNthCalledWith(2, "/api/reservations/1");
    expect(list).toEqual([{ id: 1 }]);
    expect(detail).toEqual({ id: 1 });
  });

  it("should call confirm and expire endpoints", async () => {
    axiosClient.post.mockResolvedValueOnce({ data: { status: "CONFIRMED" } });
    axiosClient.post.mockResolvedValueOnce({ data: { status: "EXPIRED" } });

    const confirmResult = await confirmReservation(5);
    const expireResult = await expireReservation(5);

    expect(axiosClient.post).toHaveBeenNthCalledWith(1, "/api/reservations/5/confirm");
    expect(axiosClient.post).toHaveBeenNthCalledWith(2, "/api/reservations/5/expire");
    expect(confirmResult.status).toBe("CONFIRMED");
    expect(expireResult.status).toBe("EXPIRED");
  });
});

