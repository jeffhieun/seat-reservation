import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { getCurrentUser } from "../api/authApi";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);

  const loadCurrentUser = async () => {
    const currentUser = await getCurrentUser();
    setUser(currentUser);
    return currentUser;
  };

  const clearUser = () => {
    setUser(null);
  };

  useEffect(() => {
    if (!localStorage.getItem("token")) {
      return;
    }

    loadCurrentUser().catch(() => {
      setUser(null);
    });
  }, []);

  const value = useMemo(() => ({
    user,
    setUser,
    loadCurrentUser,
    clearUser,
  }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
}
