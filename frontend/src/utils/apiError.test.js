import { describe, it, expect } from "vitest";
import {
  normalizeApiError,
  isUnauthorized,
  isConflict,
  isRetryable,
  getApiErrorMessage,
} from "./apiError";

describe("normalizeApiError", () => {
  it("should handle network errors", () => {
    const error = {
      message: "Network Error",
      response: null,
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(0);
    expect(result.code).toBe("NETWORK_ERROR");
    expect(result.message).toContain("Unable to connect");
    expect(result.retryable).toBe(true);
  });

  it("should handle timeout errors", () => {
    const error = {
      code: "ECONNABORTED",
      message: "timeout of 10000ms exceeded",
      response: null,
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(408);
    expect(result.code).toBe("REQUEST_TIMEOUT");
    expect(result.message).toContain("timed out");
    expect(result.retryable).toBe(true);
  });

  it("should handle 400 Bad Request with backend message", () => {
    const error = {
      response: {
        status: 400,
        data: {
          message: "Seat selection is invalid",
        },
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(400);
    expect(result.code).toBe("BAD_REQUEST");
    expect(result.message).toBe("Seat selection is invalid");
    expect(result.retryable).toBe(false);
  });

  it("should handle 400 Bad Request without backend message", () => {
    const error = {
      response: {
        status: 400,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(400);
    expect(result.code).toBe("BAD_REQUEST");
    expect(result.message).toBe("Invalid request.");
    expect(result.retryable).toBe(false);
  });

  it("should handle 401 Unauthorized with backend message", () => {
    const error = {
      response: {
        status: 401,
        data: {
          message: "Token expired",
        },
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(401);
    expect(result.code).toBe("UNAUTHORIZED");
    expect(result.message).toBe("Token expired");
    expect(result.retryable).toBe(false);
  });

  it("should handle 401 Unauthorized without backend message", () => {
    const error = {
      response: {
        status: 401,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(401);
    expect(result.code).toBe("UNAUTHORIZED");
    expect(result.message).toContain("Session expired");
    expect(result.retryable).toBe(false);
  });

  it("should handle 403 Forbidden", () => {
    const error = {
      response: {
        status: 403,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(403);
    expect(result.code).toBe("FORBIDDEN");
    expect(result.message).toContain("permission");
    expect(result.retryable).toBe(false);
  });

  it("should handle 404 Not Found", () => {
    const error = {
      response: {
        status: 404,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(404);
    expect(result.code).toBe("NOT_FOUND");
    expect(result.message).toContain("not found");
    expect(result.retryable).toBe(false);
  });

  it("should handle 409 Conflict with backend message", () => {
    const error = {
      response: {
        status: 409,
        data: {
          message: "Seat has already been reserved",
        },
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(409);
    expect(result.code).toBe("CONFLICT");
    expect(result.message).toBe("Seat has already been reserved");
    expect(result.retryable).toBe(false);
  });

  it("should handle 409 Conflict without backend message", () => {
    const error = {
      response: {
        status: 409,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(409);
    expect(result.code).toBe("CONFLICT");
    expect(result.message).toContain("already in use");
    expect(result.retryable).toBe(false);
  });

  it("should handle 422 Validation Error with backend message", () => {
    const error = {
      response: {
        status: 422,
        data: {
          message: "Invalid seat configuration",
        },
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(422);
    expect(result.code).toBe("VALIDATION_ERROR");
    expect(result.message).toBe("Invalid seat configuration");
    expect(result.retryable).toBe(false);
  });

  it("should handle 422 Validation Error without backend message", () => {
    const error = {
      response: {
        status: 422,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(422);
    expect(result.code).toBe("VALIDATION_ERROR");
    expect(result.message).toBe("Validation failed.");
    expect(result.retryable).toBe(false);
  });

  it("should handle 500 Internal Server Error", () => {
    const error = {
      response: {
        status: 500,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(500);
    expect(result.code).toBe("INTERNAL_SERVER_ERROR");
    expect(result.message).toContain("server error");
    expect(result.retryable).toBe(true);
  });

  it("should handle 503 Service Unavailable", () => {
    const error = {
      response: {
        status: 503,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(503);
    expect(result.code).toBe("SERVICE_UNAVAILABLE");
    expect(result.message).toContain("temporarily unavailable");
    expect(result.retryable).toBe(true);
  });

  it("should handle generic 5xx errors", () => {
    const error = {
      response: {
        status: 502,
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(502);
    expect(result.code).toBe("SERVER_ERROR");
    expect(result.message).toContain("error");
    expect(result.retryable).toBe(true);
  });

  it("should handle unknown error status codes", () => {
    const error = {
      response: {
        status: 418, // I'm a teapot
        data: {},
      },
    };

    const result = normalizeApiError(error);

    expect(result.status).toBe(418);
    expect(result.code).toBe("UNKNOWN_ERROR");
    expect(result.message).toBe("Something went wrong.");
    expect(result.retryable).toBe(false);
  });

  it("should handle completely missing error object", () => {
    const error = {};

    const result = normalizeApiError(error);

    expect(result.status).toBe(0);
    expect(result.code).toBe("NETWORK_ERROR");
    expect(result.retryable).toBe(true);
  });
});

describe("isUnauthorized", () => {
  it("should return true for 401 errors", () => {
    const error = {
      response: {
        status: 401,
        data: {},
      },
    };

    expect(isUnauthorized(error)).toBe(true);
  });

  it("should return false for non-401 errors", () => {
    const error = {
      response: {
        status: 403,
        data: {},
      },
    };

    expect(isUnauthorized(error)).toBe(false);
  });

  it("should return false for network errors", () => {
    const error = {
      message: "Network Error",
      response: null,
    };

    expect(isUnauthorized(error)).toBe(false);
  });
});

describe("isConflict", () => {
  it("should return true for 409 errors", () => {
    const error = {
      response: {
        status: 409,
        data: {},
      },
    };

    expect(isConflict(error)).toBe(true);
  });

  it("should return false for non-409 errors", () => {
    const error = {
      response: {
        status: 400,
        data: {},
      },
    };

    expect(isConflict(error)).toBe(false);
  });
});

describe("isRetryable", () => {
  it("should return true for network errors", () => {
    const error = {
      message: "Network Error",
      response: null,
    };

    expect(isRetryable(error)).toBe(true);
  });

  it("should return true for timeout errors", () => {
    const error = {
      code: "ECONNABORTED",
      message: "timeout of 10000ms exceeded",
      response: null,
    };

    expect(isRetryable(error)).toBe(true);
  });

  it("should return true for 500 errors", () => {
    const error = {
      response: {
        status: 500,
        data: {},
      },
    };

    expect(isRetryable(error)).toBe(true);
  });

  it("should return true for 503 errors", () => {
    const error = {
      response: {
        status: 503,
        data: {},
      },
    };

    expect(isRetryable(error)).toBe(true);
  });

  it("should return false for 4xx client errors", () => {
    const error = {
      response: {
        status: 400,
        data: {},
      },
    };

    expect(isRetryable(error)).toBe(false);
  });

  it("should return false for 401 errors", () => {
    const error = {
      response: {
        status: 401,
        data: {},
      },
    };

    expect(isRetryable(error)).toBe(false);
  });
});

describe("getApiErrorMessage (legacy method)", () => {
  it("should return normalized error message", () => {
    const error = {
      response: {
        status: 400,
        data: {
          message: "Invalid data",
        },
      },
    };

    const result = getApiErrorMessage(error, "Default");

    expect(result).toBe("Invalid data");
  });

  it("should use fallback message if error has no message", () => {
    const error = {
      response: {
        status: 500,
        data: {},
      },
    };

    const result = getApiErrorMessage(error, "Default message");

    expect(result).not.toBe("Default message");
    expect(result).toContain("error");
  });

  it("should handle network errors gracefully", () => {
    const error = {
      message: "Network Error",
      response: null,
    };

    const result = getApiErrorMessage(error, "Default");

    expect(result).toContain("Unable to connect");
  });
});
