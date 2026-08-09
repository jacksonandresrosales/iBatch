"use client";

import { useEffect, useMemo, useState } from "react";
import AppHeader from "../components/AppHeader";
import { getDashboardSummary, type DashboardSummaryResponse } from "../../lib/api";

const numberFormat = new Intl.NumberFormat("es-EC");

function statusLabel(status: string) {
  if (status === "PROCESADO_CON_RECHAZOS") return "Con rechazos";
  if (status === "ERROR") return "Error";
  return "Procesado";
}

function formatTimestamp(timestamp: string) {
  return new Intl.DateTimeFormat("es-EC", {
    dateStyle: "medium",
    timeStyle: "short",
    hour12: false,
  }).format(new Date(timestamp));
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const loadSummary = async (showNotice = false) => {
    setIsLoading(true);
    try {
      setSummary(await getDashboardSummary());
      if (showNotice) setNotice("Indicadores actualizados.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo cargar el dashboard.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void Promise.resolve().then(() => loadSummary());

    // Auto-actualización silenciosa cada 3 segundos
    const interval = setInterval(() => {
      void getDashboardSummary().then(data => setSummary(data)).catch(console.error);
    }, 3000);

    return () => clearInterval(interval);
  }, []);

  const totalTransactions = (summary?.totalProcessedTransactions ?? 0) + (summary?.totalRejectedTransactions ?? 0);
  const acceptanceRate = totalTransactions === 0
    ? 0
    : 100 - (summary?.rejectionRate ?? 0);
  const maxReasonCount = useMemo(
    () => Math.max(1, ...(summary?.rejectionReasons.map((reason) => reason.count) ?? [0])),
    [summary],
  );

  return (
    <div className="application-shell">
      <AppHeader active="dashboard" />
      <main>
        <section className="page-intro">
          <div>
            <p className="eyebrow">Control operativo / Resumen</p>
            <h1>Dashboard operativo</h1>
            <p className="page-description">Indicadores consolidados obtenidos de los archivos y transacciones registrados.</p>
          </div>
          <div className="sync-summary">
            <span className="sync-summary__label">Estado de datos</span>
            <strong>{isLoading ? "Actualizando..." : "Actualizado"}</strong>
            <button type="button" className="text-button" onClick={() => void loadSummary(true)}>
              Actualizar indicadores
            </button>
          </div>
        </section>

        {notice ? (
          <div className="notice" role="status">
            <span className="notice__line" aria-hidden="true" />
            <span>{notice}</span>
            <button type="button" onClick={() => setNotice(null)}>Cerrar</button>
          </div>
        ) : null}

        <section className="operational-overview dashboard-metrics" aria-label="Indicadores operativos">
          <div className="metric"><span>Archivos procesados</span><strong>{numberFormat.format(summary?.totalFiles ?? 0)}</strong></div>
          <div className="metric"><span>Transacciones procesadas</span><strong>{numberFormat.format(summary?.totalProcessedTransactions ?? 0)}</strong></div>
          <div className="metric"><span>Transacciones rechazadas</span><strong>{numberFormat.format(summary?.totalRejectedTransactions ?? 0)}</strong></div>
          <div className="metric"><span>Porcentaje de rechazo</span><strong>{(summary?.rejectionRate ?? 0).toFixed(2)}%</strong></div>
        </section>

        <section className="dashboard-grid dashboard-grid--primary">
          <article className="dashboard-card processing-health-card">
            <div className="dashboard-card__header"><div><p className="eyebrow">Calidad del procesamiento</p><h2>Resultado consolidado</h2></div></div>
            <div className="health-summary"><div><span className="health-summary__label">Tasa de aceptación</span><strong>{acceptanceRate.toFixed(2)}%</strong></div><span className="history-status history-status--procesado"><span aria-hidden="true" />Datos actuales</span></div>
            <div className="health-bar" aria-label={`Tasa de aceptación del ${acceptanceRate.toFixed(2)} por ciento`}><span style={{ width: `${acceptanceRate}%` }} /></div>
            <div className="health-bar__legend"><span><i className="legend-dot legend-dot--teal" /> Procesadas {numberFormat.format(summary?.totalProcessedTransactions ?? 0)}</span><span><i className="legend-dot legend-dot--copper" /> Rechazadas {numberFormat.format(summary?.totalRejectedTransactions ?? 0)}</span></div>
          </article>

          <article className="dashboard-card alerts-card">
            <div className="dashboard-card__header"><div><p className="eyebrow">Seguimiento</p><h2>Eventos recientes</h2></div><span className="alert-count">{summary?.recentEvents.length ?? 0}</span></div>
            <div className="alert-list">
              {summary?.recentEvents.length ? summary.recentEvents.map((event) => (
                <div className={`alert-item alert-item--${event.level === "ERROR" ? "error" : "warning"}`} key={event.id}>
                  <span className="alert-item__marker" aria-hidden="true" /><div><strong>{event.event}</strong><span>{event.message}</span></div><time dateTime={event.createdAt}>{formatTimestamp(event.createdAt)}</time>
                </div>
              )) : <p className="detail-empty-note">No hay eventos registrados.</p>}
            </div>
          </article>
        </section>

        <section className="dashboard-grid dashboard-grid--secondary">
          <article className="dashboard-card reasons-card">
            <div className="dashboard-card__header"><div><p className="eyebrow">Excepciones</p><h2>Motivos de rechazo</h2></div><a className="text-button dashboard-card__link" href="/files">Ver historial</a></div>
            <div className="reason-list">
              {summary?.rejectionReasons.length ? summary.rejectionReasons.map((reason) => (
                <div className="reason-row" key={reason.code}><div className="reason-row__label"><strong>{reason.name}</strong><span>{reason.code}</span></div><div className="reason-row__bar" aria-hidden="true"><span style={{ width: `${(reason.count / maxReasonCount) * 100}%` }} /></div><strong className="reason-row__count">{numberFormat.format(reason.count)}</strong></div>
              )) : <p className="detail-empty-note">No se registraron rechazos.</p>}
            </div>
          </article>

          <article className="dashboard-card recent-files-card">
            <div className="dashboard-card__header"><div><p className="eyebrow">Trazabilidad</p><h2>Últimos archivos procesados</h2></div><a className="text-button dashboard-card__link" href="/files">Ver todos</a></div>
            <div className="recent-files-list">
              {summary?.recentFiles.length ? summary.recentFiles.map((file) => (
                <a className="recent-file-row" href={`/files?selected=${file.id}`} key={file.id}><span className="recent-file-row__main"><strong>{file.fileName}</strong><span>{formatTimestamp(file.updatedAt)}</span></span><span className={`history-status history-status--${file.status.toLowerCase()}`}><span aria-hidden="true" />{statusLabel(file.status)}</span></a>
              )) : <p className="detail-empty-note">No hay archivos procesados.</p>}
            </div>
          </article>
        </section>
      </main>
    </div>
  );
}
