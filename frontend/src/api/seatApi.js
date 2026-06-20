import axiosClient from "../services/axiosClient";

export async function getSeats() {
  const response = await axiosClient.get("/api/seats");
  return response.data;
}

