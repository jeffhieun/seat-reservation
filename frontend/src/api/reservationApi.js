import axiosClient from "../services/axiosClient";

export async function reserveSeat(seatId) {
  const response = await axiosClient.post("/api/reservations", {
    seatId,
  });

  return response.data;
}

export async function getReservationById(reservationId) {
  const response = await axiosClient.get(`/api/reservations/${reservationId}`);
  return response.data;
}

export async function getUserReservations() {
  const response = await axiosClient.get("/api/reservations");
  return response.data;
}

export async function confirmReservation(reservationId) {
  const response = await axiosClient.post(`/api/reservations/${reservationId}/confirm`);
  return response.data;
}

export async function expireReservation(reservationId) {
  const response = await axiosClient.post(`/api/reservations/${reservationId}/expire`);
  return response.data;
}
