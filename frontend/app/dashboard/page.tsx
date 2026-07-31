"use client";

import { useState } from "react";
import AppHeader from "../components/AppHeader";

type DashboardStatus = "PROCESADO" | "PROCESADO_CON_RECHAZOS" | "ERROR";

type RecentFile = {
  id: string;
  name: string;
  processedAt: string;
  processed: number;
  rejected: number;
  status: DashboardStatus;
};

type RejectionReason = {
  label: string;
  code: string;
  count: number;
};

const numberFormat = new Intl.NumberFormat("es-EC");

const recentFiles: RecentFile[] = [
  {
    id: "batch-30072026",
    name: "transactions_30072026.csv",
    processedAt: "30 jul 2026 · 09:14",
    processed: 9950,
    rejected: 10,
    status: "PROCESADO_CON_RECHAZOS",
  },
  {
    id: "batch-29072026",
    name: "transactions_29072026.csv",
    processedAt: "29 jul 2026 · 09:02",
    processed: 12430,
    rejected: 0,
    status: "PROCESADO",
  },
  {
    id: "batch-28072026",
    name: "transactions_28072026.csv",
    processedAt: "28 jul 2026 · 08:27",
    processed: 0,
    rejected: 0,
    status: "ERROR",
  },
  {
    id: "batch-27072026",
    name: "transactions_27072026.csv",
    processedAt: "27 jul 2026 · 09:41",
    processed: 8184,
    rejected: 16,
    status: "PROCESADO_CON_RECHAZOS",
  },
];

const rejectionReasons: RejectionReason[] = [
  { label: "Transacción duplicada", code: "DUPLICADO", count: 82 },
  { label: "Cuenta inválida", code: "CUENTA_INVALIDA", count: 48 },
  { label: "Monto inválido", code: "MONTO_INVALIDO", count: 31 },
  { label: "Fecha inválida", code: "FECHA_INVALIDA", count: 23 },
];

const totalProcessed = 62840;
const totalRejected = 184;
const rejectionRate = ((totalRejected / (totalProcessed + totalRejected)) * 100).toFixed(2);
const maxReasonCount = Math.max(...rejectionReasons.map((reason) => reason.count));

function statusLabel(status: DashboardStatus) {
  if (status === "PROCESADO_CON_RECHAZOS") return "Con rechazos";
  if (status === "ERROR") return "Error";
  return "Procesado";
}

export default function DashboardPage() {
  const [notice, setNotice] = useState<string | null>(null);

  const refreshDashboard = () => {
    setNotice("Dashboard actualizado. Se muestran los últimos indicadores disponibles.");
  };

  return (
    <div className="application-shell">
      <AppHeader active="dashboard" />

      <main>
        <section className="page-intro">
          <div>
            <p className="eyebrow">Control operativo / Resumen</p>
            <h1>Dashboard operativo</h1>
            <p className="page-description">
              Indicadores consolidados para identificar desviaciones y priorizar la atención de los lotes.
            </p>
          </div>

          <div className="sync-summary">
            <span className="sync-summary__label">Última actualización</span>
            <strong>Hoy, 09:18</strong>
            <button type="button" className="text-button" onClick={refreshDashboard}>
              Actualizar indicadores
            </button>
          </div>
        </section>

        {notice ? (
          <div className="notice" role="status">
            <span className="notice__line" aria-hidden="true" />
            <span>{notice}</span>
            <button type="button" onClick={() => setNotice(null)}>
              Cerrar
            </button>
          </div>
        ) : null}

        <section className="operational-overview dashboard-metrics" aria-label="Indicadores operativos">
          <div className="metric">
            <span>Archivos procesados</span>
            <strong>26</strong>
            <small className="metric-trend metric-trend--positive">+4 este periodo</small>
          </div>
          <div className="metric">
            <span>Transacciones procesadas</span>
            <strong>{numberFormat.format(totalProcessed)}</strong>
            <small className="metric-trend metric-trend--positive">99,71% de aceptación</small>
          </div>
          <div className="metric">
            <span>Transacciones rechazadas</span>
            <strong>{numberFormat.format(totalRejected)}</strong>
            <small className="metric-trend metric-trend--warning">Requieren revisión</small>
          </div>
          <div className="metric">
            <span>Porcentaje de rechazo</span>
            <strong>{rejectionRate}%</strong>
            <small className="metric-trend metric-trend--neutral">Dentro del umbral operativo</small>
          </div>
        </section>

        <section className="dashboard-grid dashboard-grid--primary">
          <article className="dashboard-card processing-health-card">
            <div className="dashboard-card__header">
              <div>
                <p className="eyebrow">Calidad del procesamiento</p>
                <h2>Resultado consolidado</h2>
              </div>
              <span className="dashboard-card__period">Últimos 30 días</span>
            </div>

            <div className="health-summary">
              <div>
                <span className="health-summary__label">Tasa de aceptación</span>
                <strong>99,71%</strong>
              </div>
              <span className="history-status history-status--procesado">
                <span aria-hidden="true" />
                Operación estable
              </span>
            </div>

            <div className="health-bar" aria-label="Tasa de aceptación del 99,71 por ciento">
              <span style={{ width: "99.71%" }} />
            </div>
            <div className="health-bar__legend">
              <span><i className="legend-dot legend-dot--teal" /> Procesadas {numberFormat.format(totalProcessed)}</span>
              <span><i className="legend-dot legend-dot--copper" /> Rechazadas {numberFormat.format(totalRejected)}</span>
            </div>

            <div className="health-callout">
              <strong>Lectura operativa</strong>
              <span>La mayor concentración de rechazos proviene de transacciones duplicadas.</span>
            </div>
          </article>

          <article className="dashboard-card alerts-card">
            <div className="dashboard-card__header">
              <div>
                <p className="eyebrow">Seguimiento</p>
                <h2>Alertas recientes</h2>
              </div>
              <span className="alert-count">03</span>
            </div>

            <div className="alert-list">
              <div className="alert-item alert-item--error">
                <span className="alert-item__marker" aria-hidden="true" />
                <div>
                  <strong>Encabezado inválido</strong>
                  <span>transactions_28072026.csv</span>
                </div>
                <time dateTime="2026-07-28T08:27">28 jul</time>
              </div>
              <div className="alert-item alert-item--warning">
                <span className="alert-item__marker" aria-hidden="true" />
                <div>
                  <strong>Lote con rechazos</strong>
                  <span>10 transacciones pendientes de revisión</span>
                </div>
                <time dateTime="2026-07-30T09:14">Hoy</time>
              </div>
              <div className="alert-item alert-item--warning">
                <span className="alert-item__marker" aria-hidden="true" />
                <div>
                  <strong>Rechazos duplicados</strong>
                  <span>Concentran el 44,6% del total</span>
                </div>
                <time dateTime="2026-07-30T09:14">Hoy</time>
              </div>
            </div>
          </article>
        </section>

        <section className="dashboard-grid dashboard-grid--secondary">
          <article className="dashboard-card reasons-card">
            <div className="dashboard-card__header">
              <div>
                <p className="eyebrow">Excepciones</p>
                <h2>Motivos de rechazo más frecuentes</h2>
              </div>
              <a className="text-button dashboard-card__link" href="/files">
                Ver historial
              </a>
            </div>

            <div className="reason-list">
              {rejectionReasons.map((reason) => (
                <div className="reason-row" key={reason.code}>
                  <div className="reason-row__label">
                    <strong>{reason.label}</strong>
                    <span>{reason.code}</span>
                  </div>
                  <div className="reason-row__bar" aria-hidden="true">
                    <span style={{ width: `${(reason.count / maxReasonCount) * 100}%` }} />
                  </div>
                  <strong className="reason-row__count">{numberFormat.format(reason.count)}</strong>
                </div>
              ))}
            </div>
          </article>

          <article className="dashboard-card recent-files-card">
            <div className="dashboard-card__header">
              <div>
                <p className="eyebrow">Trazabilidad</p>
                <h2>Últimos archivos procesados</h2>
              </div>
              <a className="text-button dashboard-card__link" href="/files">
                Ver todos
              </a>
            </div>

            <div className="recent-files-list">
              {recentFiles.map((file) => (
                <a className="recent-file-row" href={`/files?selected=${file.id}`} key={file.id}>
                  <span className="recent-file-row__main">
                    <strong>{file.name}</strong>
                    <span>{file.processedAt}</span>
                  </span>
                  <span className={`history-status history-status--${file.status.toLowerCase()}`}>
                    <span aria-hidden="true" />
                    {statusLabel(file.status)}
                  </span>
                </a>
              ))}
            </div>
          </article>
        </section>

        <footer className="product-footer">
          <span>iBatch Financial Operations</span>
          <span>Visibilidad operativa y trazabilidad de lotes</span>
        </footer>
      </main>
    </div>
  );
}
