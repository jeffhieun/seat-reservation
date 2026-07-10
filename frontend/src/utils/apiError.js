/**
 * Normalize API errors into a consistent structure
 * Returns: { status, code, message, retryable }
 */
export function normalizeApiError(error) {
  // Handle timeout errors
  if (error?.code === "ECONNABORTED" || error?.message?.includes("timeout")) {
    return {
      status: 408,
      code: "REQUEST_TIMEOUT",
      message: "Request timed out.\nPlease try again.",
      retryable: true,
    };
  }

  // Handle network errors (no response from server)
  if (!error?.response) {
    return {
      status: 0,
      code: "NETWORK_ERROR",
      message:
        "Unable to connect to the server.\nPlease check your internet connection.",
      retryable: true,
    };
  }

  const status = error.response.status;
  const backendMessage = error.response.data?.message;
  const backendError = error.response.data?.error;

  // Handle 400 - Bad Request (validation errors)
  if (status === 400) {
    return {
      status: 400,
      code: "BAD_REQUEST",
      message: backendMessage || backendError || "Invalid request.",
      retryable: false,
    };
  }

  // Handle 401 - Unauthorized
  if (status === 401) {
    return {
      status: 401,
      code: "UNAUTHORIZED",
      message: backendMessage || "Session expired.\nPlease login again.",
      retryable: false,
    };
  }

  // Handle 403 - Forbidden
  if (status === 403) {
    return {
      status: 403,
      code: "FORBIDDEN",
      message: "You don't have permission to perform this action.",
      retryable: false,
    };
  }

  // Handle 404 - Not Found
  if (status === 404) {
    return {
      status: 404,
      code: "NOT_FOUND",
      message: "Requested resource was not found.",
      retryable: false,
    };
  }

  // Handle 409 - Conflict
  if (status === 409) {
    return {
      status: 409,
      code: "CONFLICT",
      message: backendMessage || "This resource is already in use.",
      retryable: false,
    };
  }

  // Handle 422 - Unprocessable Entity (validation errors)
  if (status === 422) {
    return {
      status: 422,
      code: "VALIDATION_ERROR",
      message: backendMessage || backendError || "Validation failed.",
      retryable: false,
    };
  }

  // Handle 500 - Internal Server Error
  if (status === 500) {
    return {
      status: 500,
      code: "INTERNAL_SERVER_ERROR",
      message: "Unexpected server error.\nPlease try again later.",
      retryable: true,
    };
  }

  // Handle 503 - Service Unavailable
  if (status === 503) {
    return {
      status: 503,
      code: "SERVICE_UNAVAILABLE",
      message: "Service temporarily unavailable.\nPlease try again later.",
      retryable: true,
    };
  }

  // Handle 5xx errors
  if (status >= 500) {
    return {
      status,
      code: "SERVER_ERROR",
      message: "Server error occurred.\nPlease try again later.",
      retryable: true,
    };
  }

  // Handle unknown errors
  return {
    status: status || 0,
    code: "UNKNOWN_ERROR",
    message: backendMessage || "Something went wrong.",
    retryable: false,
  };
}

/**
 * Check if error is unauthorized (401)
 */
export function isUnauthorized(error) {
  const normalized = normalizeApiError(error);
  return normalized.status === 401;
}

/**
 * Check if error is a conflict (409)
 */
export function isConflict(error) {
  const normalized = normalizeApiError(error);
  return normalized.status === 409;
}

/**
 * Check if error is retryable
 */
export function isRetryable(error) {
  const normalized = normalizeApiError(error);
  return normalized.retryable;
}

/**
 * Legacy method for backward compatibility
 */
export function getApiErrorMessage(error, fallbackMessage) {
  const normalized = normalizeApiError(error);
  return normalized.message || fallbackMessage;
}
