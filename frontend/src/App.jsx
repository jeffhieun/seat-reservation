import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import SeatsPage from "./pages/SeatsPage";
import PaymentPage from "./pages/PaymentPage";
import SuccessPage from "./pages/SuccessPage";
import ReservationPage from "./pages/ReservationPage";

function ProtectedRoute({ children }) {
  const token = localStorage.getItem("token");

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/auth/register" element={<RegisterPage />} />
      <Route
        path="/seats"
        element={(
          <ProtectedRoute>
            <SeatsPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/reservations/:reservationId"
        element={(
          <ProtectedRoute>
            <ReservationPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/payment/:reservationId"
        element={(
          <ProtectedRoute>
            <PaymentPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/success"
        element={(
          <ProtectedRoute>
            <SuccessPage />
          </ProtectedRoute>
        )}
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
