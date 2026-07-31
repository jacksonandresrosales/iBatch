"use client";

import { useMemo, useState } from "react";
import AppHeader from "../components/AppHeader";

type AuditSeverity = "INFO" | "WARN" | "ERROR";

type AuditEvent = {
  id: string;
  timestamp: string;
  isoTimestamp: string;
  severity: AuditSeverity;
  event: string;
  file: string;
  message: string;
  source: string;
  correlationId: string;
};

const initialAuditEvents: AuditEvent[] = [
  {
    id: "evt-0918-01",
    timestamp: "30 jul 2026 · 09:18:04",
    isoTimestamp: "2026-07-30T09:18:04",
    severity: "INFO",
    event: "PROCESAMIENTO_FINALIZADO",
    file: "transactions_30072026.csv",
    message: "Lote procesado correctamente con 10 rechazos registrados.",
    source: "BatchProcessor",
    correlationId: "run-30072026-0914",
  },
  {
    id: "evt-0914-02",
    timestamp: "30 jul 2026 · 09:14:22",
    isoTimestamp: "2026-07-30T09:14:22",
    severity: "WARN",
    event: "TRANSACCIONES_RECHAZADAS",
    file: "transactions_30072026.csv",
    message: "10 transacciones rechazadas por reglas de validación.",
    source: "ValidationService",
    correlationId: "run-30072026-0914",
  },
  {
    id: "evt-0841-03",
    timestamp: "30 jul 2026 · 08:41:09",
    isoTimestamp: "2026-07-30T08:41:09",
    severity: "INFO",
    event: "ARCHIVO_DETECTADO",
    file: "transactions_30072026.csv",
    message: "Archivo disponible en el directorio de entrada autorizado.",
    source: "FileWatcher",
    correlationId: "scan-30072026-0841",
  },
  {
    id: "evt-0827-04",
    timestamp: "28 jul 2026 · 08:27:18",
    isoTimestamp: "2026-07-28T08:27:18",
    severity: "ERROR",
    event: "ARCHIVO_INVALIDO",
    file: "transactions_28072026.csv",
    message: "Encabezado inválido. El lote no pudo iniciar procesamiento.",
    source: "FileValidator",
    correlationId: "run-28072026-0827",
  },
  {
    id: "evt-0827-05",
    timestamp: "28 jul 2026 · 08:27:17",
    isoTimestamp: "2026-07-28T08:27:17",
    severity: "WARN",
    event: "VALIDACION_DETENIDA",
    file: "transactions_28072026.csv",
    message: "La estructura del archivo no coincide con el formato esperado.",
    source: "FileValidator",
    correlationId: "run-28072026-0827",
  },
  {
    id: "evt-0902-06",
    timestamp: "29 jul 2026 · 09:02:46",
    isoTimestamp: "2026-07-29T09:02:46",
    severity: "INFO",
    event: "PROCESAMIENTO_FINALIZADO",
    file: "transactions_29072026.csv",
    message: "12.430 transacciones procesadas sin rechazos.",
    source: "BatchProcessor",
    correlationId: "run-29072026-0902",
  },
];

const severityLabels: Record<AuditSeverity, string> = {
  INFO: "Información",
  WARN: "Advertencia",
  ERROR: "Error",
};

function formatLogLine(event: AuditEvent) {
  return [
    event.timestamp,
    event.severity,
    event.event,
    `archivo=${event.file}`,
    `origen=${event.source}`,
    `correlacion=${event.correlationId}`,
    event.message,
  ].join(" | ");
}

export default function AuditPage() {
  const [query, setQuery] = useState("");
  const [severityFilter, setSeverityFilter] = useState<"TODOS" | AuditSeverity>(
    "TODOS",
  );
  const [notice, setNotice] = useState<string | null>(null);

  const visibleEvents = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return initialAuditEvents.filter((event) => {
      const matchesQuery = [event.event, event.file, event.message, event.source]
        .join(" ")
        .toLowerCase()
        .includes(normalizedQuery);
      const matchesSeverity =
        severityFilter === "TODOS" || event.severity === severityFilter;
      return matchesQuery && matchesSeverity;
    });
  }, [query, severityFilter]);

  const exportLogs = () => {
    const header = [
      "iBatch Financial Operations",
      "Exportación de auditoría operativa",
      `Generado: ${new Intl.DateTimeFormat("es-EC", {
        dateStyle: "short",
        timeStyle: "medium",
      }).format(new Date())}`,
      "",
    ].join("\n");
    const content = `${header}${visibleEvents.map(formatLogLine).join("\n")}`;
    const blob = new Blob(["\ufeff", content], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "logs.txt";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    setNotice(`Se exportaron ${visibleEvents.length} eventos en logs.txt.`);
  };

  const infoCount = visibleEvents.filter((event) => event.severity === "INFO").length;
  const warningCount = visibleEvents.filter((event) => event.severity === "WARN").length;
  const errorCount = visibleEvents.filter((event) => event.severity === "ERROR").length;

  return (
    <div className="application-shell">
      <AppHeader active="audit" />

      <main>
        <section className="page-intro">
          <div>
            <p className="eyebrow">Control operativo / Trazabilidad</p>
            <h1>Auditoría operativa</h1>
            <p className="page-description">
              Consulte los eventos relevantes del procesamiento y conserve evidencia exportable de cada ejecución.
            </p>
          </div>

          <div className="sync-summary">
            <span className="sync-summary__label">Última actividad</span>
            <strong>Hoy, 09:18</strong>
            <span className="audit-summary-note">6 eventos disponibles</span>
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

        <section className="operational-overview audit-metrics" aria-label="Resumen de auditoría">
          <div className="metric">
            <span>Eventos visibles</span>
            <strong>{visibleEvents.length.toString().padStart(2, "0")}</strong>
          </div>
          <div className="metric">
            <span>Información</span>
            <strong>{infoCount.toString().padStart(2, "0")}</strong>
          </div>
          <div className="metric">
            <span>Advertencias</span>
            <strong className="audit-metric__warning">{warningCount.toString().padStart(2, "0")}</strong>
          </div>
          <div className="metric">
            <span>Errores</span>
            <strong className="audit-metric__error">{errorCount.toString().padStart(2, "0")}</strong>
          </div>
        </section>

        <section className="file-panel audit-panel">
          <div className="panel-header audit-panel__header">
            <div>
              <h2>Registro de eventos</h2>
              <p>Eventos técnicos y operativos asociados a los lotes procesados.</p>
            </div>
            <div className="audit-actions">
              <label className="search-field">
                <span>Buscar evento</span>
                <input
                  type="search"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Archivo, evento u origen"
                />
              </label>
              <label className="filter-field">
                <span>Severidad</span>
                <select
                  value={severityFilter}
                  onChange={(event) =>
                    setSeverityFilter(event.target.value as "TODOS" | AuditSeverity)
                  }
                >
                  <option value="TODOS">Todas</option>
                  <option value="INFO">Información</option>
                  <option value="WARN">Advertencias</option>
                  <option value="ERROR">Errores</option>
                </select>
              </label>
              <button
                type="button"
                className="primary-button audit-export-button"
                onClick={exportLogs}
                disabled={visibleEvents.length === 0}
              >
                Descargar log.txt
              </button>
            </div>
          </div>

          <div className="table-wrapper">
            <table className="audit-table">
              <thead>
                <tr>
                  <th>Fecha y hora</th>
                  <th>Severidad</th>
                  <th>Evento</th>
                  <th>Archivo</th>
                  <th>Detalle</th>
                  <th>Origen</th>
                </tr>
              </thead>
              <tbody>
                {visibleEvents.map((event) => (
                  <tr key={event.id}>
                    <td className="audit-timestamp">
                      <time dateTime={event.isoTimestamp}>{event.timestamp}</time>
                      <span>{event.correlationId}</span>
                    </td>
                    <td>
                      <span className={`audit-severity audit-severity--${event.severity.toLowerCase()}`}>
                        <span aria-hidden="true" />
                        {severityLabels[event.severity]}
                      </span>
                    </td>
                    <td>
                      <strong className="audit-event">{event.event}</strong>
                    </td>
                    <td>
                      <span className="file-name audit-file-name">{event.file}</span>
                    </td>
                    <td>
                      <span className="audit-message">{event.message}</span>
                    </td>
                    <td>
                      <span className="audit-source">{event.source}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {visibleEvents.length === 0 ? (
              <div className="empty-state">
                <strong>No se encontraron eventos</strong>
                <span>Ajuste la búsqueda o el filtro de severidad.</span>
              </div>
            ) : null}
          </div>

          <div className="panel-footer">
            <span>{visibleEvents.length} de {initialAuditEvents.length} eventos visibles</span>
            <span className="audit-panel__hint">
              La exportación respeta los filtros aplicados.
            </span>
          </div>
        </section>

        <footer className="product-footer">
          <span>iBatch Financial Operations</span>
          <span>Auditoría operativa y trazabilidad de procesos</span>
        </footer>
      </main>
    </div>
  );
}
