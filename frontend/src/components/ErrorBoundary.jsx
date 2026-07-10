import { Component } from "react";
import { useNavigate } from "react-router-dom";
import "./ErrorBoundary.css";

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
    };
  }

  static getDerivedStateFromError(error) {
    return {
      hasError: true,
      error,
    };
  }

  componentDidCatch(error, errorInfo) {
    // Log error for debugging and monitoring
    console.error("Error caught by ErrorBoundary:", error);
    console.error("Error info:", errorInfo);

    // Future: Replace with monitoring service (Sentry, Datadog, etc.)
    // Example:
    // sentryClient.captureException(error, { contexts: { react: errorInfo } });
  }

  handleTryAgain = () => {
    this.setState({
      hasError: false,
      error: null,
    });
  };

  handleBackToHome = () => {
    // Reset state and navigate to home
    this.setState({
      hasError: false,
      error: null,
    });
    // Navigate using window.location since we can't use useNavigate in class component
    window.location.href = "/";
  };

  render() {
    if (this.state.hasError) {
      return (
        <main className="error-boundary-container">
          <div className="error-boundary-content">
            <div className="error-boundary-icon">⚠️</div>
            <h1 className="error-boundary-title">Something went wrong</h1>
            <p className="error-boundary-description">
              An unexpected error occurred while rendering this page.
              {" "}
              Please try again or go back to the home page.
            </p>

            <div className="error-boundary-actions">
              <button
                type="button"
                className="btn"
                onClick={this.handleTryAgain}
              >
                Try Again
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={this.handleBackToHome}
              >
                Back to Home
              </button>
            </div>
          </div>
        </main>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
