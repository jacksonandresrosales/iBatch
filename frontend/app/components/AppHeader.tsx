type ActiveNavigation = "operations" | "history";

type AppHeaderProps = {
  active: ActiveNavigation;
};

export default function AppHeader({ active }: AppHeaderProps) {
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
        <span className="nav-link nav-link--disabled">Dashboard</span>
      </nav>

      <div className="environment-status" aria-label="Estado del sistema">
        <span className="status-dot" aria-hidden="true" />
        <span>
          <small>Ambiente local</small>
          <strong>Operativo</strong>
        </span>
      </div>
    </header>
  );
}
