import axiosClient from "../services/axiosClient";

export async function login(email, password) {
  const response = await axiosClient.post("/api/auth/login", {
    email,
    password,
  });

  return response.data;
}

export async function register(email, password) {
  const response = await axiosClient.post("/api/auth/register", {
    email,
    password,
  });

  return response.data;
}

