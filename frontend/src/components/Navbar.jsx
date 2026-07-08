import { useNavigate } from "react-router-dom";
import "./Navbar.css";

function Navbar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userEmail");
    navigate("/login", { replace: true });
  };

  return (
    <header className="navbar">
      <h1 className="navbar-title">Seat Reservation Platform</h1>
      <button className="navbar-logout" onClick={handleLogout} type="button">
        Logout
      </button>
    </header>
  );
}

export default Navbar;
