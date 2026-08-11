"use client";

import { FormEvent, useState } from "react";

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const submitLogin = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setNotice("La interfaz está lista. La autenticación se conectará en la siguiente etapa.");
  };

  return (
    <div className="login-shell">
      <section className="login-brand-panel" aria-labelledby="login-brand-title">
        <a className="brand login-brand" href="/login" aria-label="iBatch, acceso">
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

        <div className="login-brand-copy">
          <h1 id="login-brand-title">Cada lote entra con control. Cada resultado sale con evidencia.</h1>
          <p>
            Carga, procesa y audita transacciones financieras desde una sola superficie operativa.
          </p>
        </div>

        <dl className="login-capabilities" aria-label="Capacidades de iBatch">
          <div>
            <dt>Carga</dt>
            <dd>Archivos CSV autorizados</dd>
          </div>
          <div>
            <dt>Proceso</dt>
            <dd>Validación y progreso</dd>
          </div>
          <div>
            <dt>Auditoría</dt>
            <dd>Eventos y rechazos</dd>
          </div>
        </dl>

        <div className="login-brand-geometry" aria-hidden="true">
          <span className="login-brand-geometry__navy" />
          <span className="login-brand-geometry__teal" />
          <span className="login-brand-geometry__copper" />
        </div>

        <footer className="login-brand-footer">
          <span>iBatch Financial Operations</span>
          <span>Acceso para personal autorizado</span>
        </footer>
      </section>

      <main className="login-main">
        <section className="login-form-panel" aria-labelledby="login-title">
          <div className="login-form-heading">
            <h2 id="login-title">Accede a iBatch</h2>
            <p>Ingresa con las credenciales asignadas para continuar al panel operativo.</p>
          </div>

          <form className="login-form" onSubmit={submitLogin}>
            <div className="login-field">
              <label htmlFor="username">Usuario o correo electrónico</label>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                placeholder="nombre@empresa.com"
                required
              />
            </div>

            <div className="login-field">
              <div className="login-field__label-row">
                <label htmlFor="password">Contraseña</label>
                <button
                  type="button"
                  className="login-password-toggle"
                  aria-controls="password"
                  aria-pressed={showPassword}
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? "Ocultar" : "Mostrar"}
                </button>
              </div>
              <input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                placeholder="Ingresa tu contraseña"
                required
              />
            </div>

            {notice ? (
              <p className="login-notice" role="status" aria-live="polite">
                {notice}
              </p>
            ) : null}

            <button type="submit" className="login-submit">
              Ingresar al sistema
            </button>
          </form>

          <p className="login-help">
            ¿No puedes acceder? Solicita ayuda al administrador del sistema.
          </p>
        </section>
      </main>
    </div>
  );
}
