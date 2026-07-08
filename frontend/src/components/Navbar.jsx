import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Navbar.css";

function Navbar() {
  const navigate = useNavigate();
  const { clearUser, user } = useAuth();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userEmail");
    clearUser();
    navigate("/login", { replace: true });
  };

  return (
    <header className="navbar">
      <h1 className="navbar-title">Seat Reservation Platform</h1>
      {user?.email ? <span className="navbar-user">{user.email}</span> : null}
      <button className="navbar-logout" onClick={handleLogout} type="button">
        Logout
      </button>
    </header>
  );
}

export default Navbar;
