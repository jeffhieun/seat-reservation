import { useNavigate } from "react-router-dom";

function Navbar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userEmail");
    navigate("/login", { replace: true });
  };

  return (
    <header className="navbar">
      <h1 className="navbar-title">Seat Reservation Platform</h1>
      <button className="btn btn-secondary" onClick={handleLogout} type="button">
        Logout
      </button>
    </header>
  );
}

export default Navbar;

