import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ErrorBoundary from "./ErrorBoundary";

// Mock child component that throws an error
const ThrowError = () => {
  throw new Error("Test error");
};

// Normal child component
const NormalChild = () => {
  return <div data-testid="normal-child">Normal content</div>;
};

describe("ErrorBoundary", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Mock console.error to prevent noise in test output
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("renders children normally when there is no error", () => {
    render(
      <ErrorBoundary>
        <NormalChild />
      </ErrorBoundary>
    );

    const normalChild = screen.getByTestId("normal-child");
    expect(normalChild).toBeInTheDocument();
    expect(normalChild).toHaveTextContent("Normal content");
  });

  it("displays fallback UI when child component throws an error", () => {
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
    expect(
      screen.getByText(
        /An unexpected error occurred while rendering this page/
      )
    ).toBeInTheDocument();
  });

  it("does not expose stack trace in the fallback UI", () => {
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    // The error message should not contain the actual error details
    const description = screen.getByText(
      /An unexpected error occurred while rendering this page/
    );
    expect(description.textContent).not.toContain("Test error");
  });

  it("displays Try Again button in fallback UI", () => {
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    const tryAgainButton = screen.getByRole("button", { name: "Try Again" });
    expect(tryAgainButton).toBeInTheDocument();
  });

  it("displays Back to Home button in fallback UI", () => {
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    const backButton = screen.getByRole("button", { name: "Back to Home" });
    expect(backButton).toBeInTheDocument();
  });

  it("resets error state when Try Again button is clicked", () => {
    // Create an instance-based component we can control
    let shouldThrow = true;

    const ToggleThrow = () => {
      if (shouldThrow) {
        throw new Error("Intentional error for testing");
      }
      return <div data-testid="recovered-content">Success!</div>;
    };

    // Initial render with error
    const { rerender } = render(
      <ErrorBoundary>
        <ToggleThrow />
      </ErrorBoundary>
    );

    // Error UI should be visible
    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
    const tryAgainButton = screen.getByRole("button", { name: "Try Again" });
    expect(tryAgainButton).toBeInTheDocument();

    // Stop throwing
    shouldThrow = false;

    // Click Try Again
    fireEvent.click(tryAgainButton);

    // Rerender - now ToggleThrow won't throw
    rerender(
      <ErrorBoundary>
        <ToggleThrow />
      </ErrorBoundary>
    );

    // Error state should be cleared and normal content should show
    expect(screen.queryByText("Something went wrong")).not.toBeInTheDocument();
    expect(screen.getByTestId("recovered-content")).toBeInTheDocument();
  });

  it("navigates to home when Back to Home button is clicked", () => {
    // Mock window.location.href
    const originalLocation = window.location;
    delete window.location;
    window.location = { href: "" };

    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    const backButton = screen.getByRole("button", { name: "Back to Home" });
    fireEvent.click(backButton);

    expect(window.location.href).toBe("/");

    // Restore original window.location
    window.location = originalLocation;
  });

  it("logs error when componentDidCatch is triggered", () => {
    const consoleErrorSpy = console.error;

    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    // Verify console.error was called with the error message
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Error caught by ErrorBoundary"),
      expect.any(Error)
    );
  });

  it("renders error icon in fallback UI", () => {
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    const icon = screen.getByText("⚠️");
    expect(icon).toBeInTheDocument();
  });

  it("has correct CSS classes for styling", () => {
    const { container } = render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    expect(container.querySelector(".error-boundary-container")).toBeInTheDocument();
    expect(container.querySelector(".error-boundary-content")).toBeInTheDocument();
    expect(container.querySelector(".error-boundary-icon")).toBeInTheDocument();
    expect(container.querySelector(".error-boundary-title")).toBeInTheDocument();
    expect(container.querySelector(".error-boundary-description")).toBeInTheDocument();
    expect(container.querySelector(".error-boundary-actions")).toBeInTheDocument();
  });

  it("maintains error state correctly after multiple renders", () => {
    const { rerender } = render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    // First render shows error
    expect(screen.getByText("Something went wrong")).toBeInTheDocument();

    // Rerender with same props
    rerender(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    );

    // Error should still be visible
    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
  });
});
