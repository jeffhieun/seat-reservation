import "./LoadingSpinner.css";

function LoadingSpinner({ size = "medium", message = "Loading...", fullscreen = false, className = "" }) {
  const spinnerClass = `loading-spinner loading-spinner-${size} ${fullscreen ? "loading-spinner-fullscreen" : ""} ${className}`.trim();

  return (
    <div className={spinnerClass} role="status" aria-live="polite" aria-busy="true">
      <div className="loading-spinner-animation" />
      {message && <p className="loading-spinner-message">{message}</p>}
    </div>
  );
}

export default LoadingSpinner;
