"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

import { login } from "../../lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submitLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const username = String(formData.get("username") ?? "").trim();
    const password = String(formData.get("password") ?? "");

    setIsSubmitting(true);
    setNotice(null);
    try {
      await login(username, password);
      router.replace("/files/available");
      router.refresh();
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo iniciar sesión");
    } finally {
      setIsSubmitting(false);
    }
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

          <form className="login-form" aria-busy={isSubmitting} onSubmit={submitLogin}>
            <div className="login-field">
              <label htmlFor="username">Usuario</label>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                placeholder="Ingresa tu usuario"
                maxLength={100}
                disabled={isSubmitting}
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
                  disabled={isSubmitting}
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
                maxLength={72}
                disabled={isSubmitting}
                required
              />
            </div>

            {notice ? (
              <p className="login-notice login-notice--error" role="alert">
                {notice}
              </p>
            ) : null}

            <button type="submit" className="login-submit" disabled={isSubmitting}>
              {isSubmitting ? "Validando acceso..." : "Ingresar al sistema"}
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
