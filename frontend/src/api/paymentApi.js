import axiosClient from "../services/axiosClient";

export async function initiatePayment(reservationId) {
  const response = await axiosClient.post("/api/payments", null, {
    params: { reservationId },
  });

  return response.data;
}

export async function getPaymentById(paymentId) {
  const response = await axiosClient.get(`/api/payments/${paymentId}`);
  return response.data;
}

export async function completePayment(paymentId, result = "SUCCESS") {
  const response = await axiosClient.post(`/api/payments/${paymentId}`, {
    result,
  });
  return response.data;
}

export async function completePaymentForTesting(paymentId, result = "SUCCESS") {
  const response = await axiosClient.post(`/api/payments/${paymentId}/complete`, {
    result,
  });
  return response.data;
}
