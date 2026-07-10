import axios from "axios";
import { normalizeApiError } from "../utils/apiError.js";

const BASE_URL = "http://localhost:8080";

const axiosClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

function logoutAndRedirect() {
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("userEmail");

  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

let refreshPromise = null;

axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error?.config;

    if (error?.response?.status !== 401 || !originalRequest) {
      return Promise.reject(normalizeApiError(error));
    }

    const requestUrl = originalRequest?.url || "";
    const refreshToken = localStorage.getItem("refreshToken");

    if (
      originalRequest._retry ||
      requestUrl.includes("/api/auth/refresh") ||
      !refreshToken
    ) {
      logoutAndRedirect();
      return Promise.reject(normalizeApiError(error));
    }

    originalRequest._retry = true;

    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post(
            `${BASE_URL}/api/auth/refresh`,
            { refreshToken },
            {
              headers: { "Content-Type": "application/json" },
            }
          )
          .then((response) => {
            const newAccessToken = response?.data?.token;
            const newRefreshToken = response?.data?.refreshToken;

            if (!newAccessToken || !newRefreshToken) {
              throw new Error("Invalid refresh response");
            }

            localStorage.setItem("token", newAccessToken);
            localStorage.setItem("refreshToken", newRefreshToken);
          })
          .finally(() => {
            refreshPromise = null;
          });
      }

      await refreshPromise;
      originalRequest.headers = originalRequest.headers || {};
      originalRequest.headers.Authorization = `Bearer ${localStorage.getItem("token")}`;
      return axiosClient(originalRequest);
    } catch (refreshError) {
      logoutAndRedirect();
      return Promise.reject(normalizeApiError(refreshError));
    }
  }
);

export default axiosClient;
