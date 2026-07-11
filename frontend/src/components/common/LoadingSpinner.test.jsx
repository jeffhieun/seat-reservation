import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import LoadingSpinner from "./LoadingSpinner";

describe("LoadingSpinner", () => {
  it("should render successfully with default props", () => {
    render(<LoadingSpinner />);
    const spinner = screen.getByRole("status");
    expect(spinner).toBeInTheDocument();
  });

  it("should display default message 'Loading...' when no message prop is provided", () => {
    render(<LoadingSpinner />);
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("should display custom message when message prop is provided", () => {
    render(<LoadingSpinner message="Loading seats..." />);
    expect(screen.getByText("Loading seats...")).toBeInTheDocument();
  });

  it("should not display message when message prop is empty string", () => {
    render(<LoadingSpinner message="" />);
    const spinner = screen.getByRole("status");
    expect(spinner.textContent).toBe("");
  });

  it("should render small size spinner", () => {
    render(<LoadingSpinner size="small" message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).toHaveClass("loading-spinner-small");
  });

  it("should render medium size spinner by default", () => {
    render(<LoadingSpinner message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).toHaveClass("loading-spinner-medium");
  });

  it("should render large size spinner", () => {
    render(<LoadingSpinner size="large" message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).toHaveClass("loading-spinner-large");
  });

  it("should apply fullscreen class when fullscreen prop is true", () => {
    render(<LoadingSpinner fullscreen message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).toHaveClass("loading-spinner-fullscreen");
  });

  it("should not apply fullscreen class when fullscreen prop is false", () => {
    render(<LoadingSpinner fullscreen={false} message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).not.toHaveClass("loading-spinner-fullscreen");
  });

  it("should apply custom className", () => {
    render(<LoadingSpinner className="custom-class" message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).toHaveClass("custom-class");
  });

  it("should have correct accessibility attributes", () => {
    render(<LoadingSpinner message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).toHaveAttribute("role", "status");
    expect(spinner).toHaveAttribute("aria-live", "polite");
    expect(spinner).toHaveAttribute("aria-busy", "true");
  });

  it("should render animation div", () => {
    const { container } = render(<LoadingSpinner message="Loading..." />);
    const animationDiv = container.querySelector(".loading-spinner-animation");
    expect(animationDiv).toBeInTheDocument();
  });

  it("should combine multiple size and style classes correctly", () => {
    render(<LoadingSpinner size="large" fullscreen className="extra-class" message="Loading..." />);
    const spinner = screen.getByRole("status");
    expect(spinner).toHaveClass("loading-spinner");
    expect(spinner).toHaveClass("loading-spinner-large");
    expect(spinner).toHaveClass("loading-spinner-fullscreen");
    expect(spinner).toHaveClass("extra-class");
  });

  it("should handle message with special characters", () => {
    render(<LoadingSpinner message="Loading data... (50%)" />);
    expect(screen.getByText("Loading data... (50%)")).toBeInTheDocument();
  });

  it("should handle very long message", () => {
    const longMessage = "This is a very long loading message that might wrap to multiple lines on smaller screens.";
    render(<LoadingSpinner message={longMessage} />);
    expect(screen.getByText(longMessage)).toBeInTheDocument();
  });
});
