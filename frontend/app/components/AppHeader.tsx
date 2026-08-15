"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { getAuthenticatedUser, logout, type AuthenticatedUserResponse } from "../../lib/api";

type ActiveNavigation = "operations" | "history" | "dashboard" | "audit";

type AppHeaderProps = {
  active: ActiveNavigation;
};

export default function AppHeader({ active }: AppHeaderProps) {
  const router = useRouter();
  const [user, setUser] = useState<AuthenticatedUserResponse | null>(null);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutFailed, setLogoutFailed] = useState(false);

  useEffect(() => {
    void getAuthenticatedUser().then(setUser).catch(() => undefined);
  }, []);

  const closeSession = async () => {
    setIsLoggingOut(true);
    setLogoutFailed(false);
    try {
      await logout();
      router.replace("/login");
      router.refresh();
    } catch {
      setLogoutFailed(true);
      setIsLoggingOut(false);
    }
  };

  return (
    <header className="topbar">
      <a className="brand" href="/files/available" aria-label="iBatch, inicio">
        <span className="brand-mark" aria-hidden="true">
          <span className="brand-mark__navy" />
          <span className="brand-mark__teal" />
          <span className="brand-mark__copper" />
        </span>
        <span className="brand-copy">
          <strong>iBatch</strong>
          <small>Financial Operations</small>
        </span>
      </a>

      <nav className="primary-navigation" aria-label="Navegación principal">
        <a
          className={`nav-link ${active === "operations" ? "nav-link--active" : ""}`}
          href="/files/available"
        >
          Operaciones
        </a>
        <a
          className={`nav-link ${active === "history" ? "nav-link--active" : ""}`}
          href="/files"
        >
          Historial
        </a>
        <a
          className={`nav-link ${active === "dashboard" ? "nav-link--active" : ""}`}
          href="/dashboard"
        >
          Dashboard
        </a>
        <a
          className={`nav-link ${active === "audit" ? "nav-link--active" : ""}`}
          href="/audit"
        >
          Auditoría
        </a>
      </nav>

      <div className="environment-status" aria-label="Sesión actual">
        <span className="status-dot" aria-hidden="true" />
        <span className="session-identity" aria-live="polite">
          <small>
            {user?.role === "ADMIN"
              ? "Administrador"
              : user?.role === "OPERATOR"
                ? "Operador"
                : "Sesión"}
          </small>
          <strong title={user?.username}>{user?.username ?? "Verificando acceso"}</strong>
        </span>
        {user ? (
          <button
            type="button"
            className="logout-button"
            disabled={isLoggingOut}
            onClick={closeSession}
          >
            {isLoggingOut ? "Saliendo..." : logoutFailed ? "Reintentar" : "Salir"}
          </button>
        ) : null}
        {logoutFailed ? (
          <span className="sr-only" role="alert">No se pudo cerrar la sesión. Inténtelo nuevamente.</span>
        ) : null}
      </div>
    </header>
  );
}
