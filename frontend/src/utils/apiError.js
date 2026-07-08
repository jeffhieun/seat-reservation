export function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message
    || fallbackMessage;
}
