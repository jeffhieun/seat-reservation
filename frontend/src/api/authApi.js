import axiosClient from "../services/axiosClient";

export async function login(email, password) {
  const response = await axiosClient.post("/api/auth/login", {
    email,
    password,
  });

  if (response.data?.refreshToken) {
    localStorage.setItem("refreshToken", response.data.refreshToken);
  }

  return response.data;
}

export async function register(email, password) {
  const response = await axiosClient.post("/api/auth/register", {
    email,
    password,
  });

  return response.data;
}

export async function getCurrentUser() {
  const response = await axiosClient.get("/api/auth/me");
  return response.data;
}
