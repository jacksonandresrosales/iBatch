"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AppHeader from "../components/AppHeader";
import { getProcessingLogs, type ProcessingLogResponse } from "../../lib/api";

type AuditSeverity = "INFO" | "WARNING" | "ERROR" | "SUCCESS";

const severityLabels: Record<AuditSeverity, string> = {
  INFO: "Información",
  SUCCESS: "Éxito",
  WARNING: "Advertencia",
  ERROR: "Error",
};

function formatTimestamp(timestamp: string) {
  return new Intl.DateTimeFormat("es-EC", {
    dateStyle: "medium",
    timeStyle: "medium",
    hour12: false,
  }).format(new Date(timestamp));
}

function formatLogLine(event: ProcessingLogResponse) {
  return [
    formatTimestamp(event.createdAt),
    event.level,
    event.event,
    `archivo=${event.fileName ?? "no asociado"}`,
    event.message,
  ].join(" | ");
}

export default function AuditPage() {
  const [events, setEvents] = useState<ProcessingLogResponse[]>([]);
  const [query, setQuery] = useState("");
  const [severityFilter, setSeverityFilter] = useState<"TODOS" | AuditSeverity>("TODOS");
  const [notice, setNotice] = useState<string | null>(null);

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<25 | 50>(50);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const loadLogs = useCallback(async (showNotice = false, currentPage = page, currentSize = pageSize) => {
    try {
      const data = await getProcessingLogs(currentPage, currentSize);
      setEvents(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
      if (showNotice) setNotice("Auditoría actualizada.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo cargar la auditoría.");
    }
  }, [page, pageSize]);

  useEffect(() => {
    void Promise.resolve().then(() => loadLogs(false, page, pageSize));

    // Auto-actualización silenciosa cada 3 segundos para la página actual
    const interval = setInterval(() => {
      void getProcessingLogs(page, pageSize).then(data => {
        setEvents(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      }).catch(console.error);
    }, 3000);

    return () => clearInterval(interval);
  }, [loadLogs, page, pageSize]);

  const visibleEvents = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return events.filter((event) => {
      const matchesQuery = [event.event, event.fileName, event.message].join(" ").toLowerCase().includes(normalizedQuery);
      return matchesQuery && (severityFilter === "TODOS" || event.level === severityFilter);
    });
  }, [events, query, severityFilter]);

  const exportLogs = () => {
    const content = ["iBatch Financial Operations", "Exportación de auditoría", "", ...visibleEvents.map(formatLogLine)].join("\n");
    const blob = new Blob(["\ufeff", content], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "logs.txt";
    anchor.click();
    URL.revokeObjectURL(url);
    setNotice(`Se exportaron ${visibleEvents.length} eventos en logs.txt.`);
  };

  const countByLevel = (level: AuditSeverity) => visibleEvents.filter((event) => event.level === level).length;

  return (
    <div className="application-shell">
      <AppHeader active="audit" />
      <main>
        <section className="page-intro">
          <div><p className="eyebrow">Control operativo / Trazabilidad</p><h1>Auditoría operativa</h1><p className="page-description">Eventos reales persistidos durante el procesamiento y reproceso de archivos.</p></div>
          <div className="sync-summary"><span className="sync-summary__label">Total eventos en BD</span><strong>{totalElements}</strong><button type="button" className="text-button" onClick={() => void loadLogs(true)}>Actualizar auditoría</button></div>
        </section>

        {notice ? <div className="notice" role="status"><span className="notice__line" aria-hidden="true" /><span>{notice}</span><button type="button" onClick={() => setNotice(null)}>Cerrar</button></div> : null}

        <section className="operational-overview audit-metrics" aria-label="Resumen de auditoría">
          <div className="metric"><span>Eventos visibles</span><strong>{visibleEvents.length}</strong></div>
          <div className="metric"><span>Información</span><strong>{countByLevel("INFO")}</strong></div>
          <div className="metric"><span>Advertencias</span><strong className="audit-metric__warning">{countByLevel("WARNING")}</strong></div>
          <div className="metric"><span>Errores</span><strong className="audit-metric__error">{countByLevel("ERROR")}</strong></div>
        </section>

        <section className="file-panel audit-panel">
          <div className="panel-header audit-panel__header">
            <div><h2>Registro de eventos</h2><p>Eventos técnicos y operativos asociados a los lotes procesados.</p></div>
            <div className="audit-actions">
              <label className="search-field"><span>Buscar evento</span><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Archivo, evento o detalle" /></label>
              <label className="filter-field"><span>Severidad</span><select value={severityFilter} onChange={(event) => setSeverityFilter(event.target.value as "TODOS" | AuditSeverity)}><option value="TODOS">Todas</option><option value="INFO">Información</option><option value="SUCCESS">Éxito</option><option value="WARNING">Advertencias</option><option value="ERROR">Errores</option></select></label>
              <button type="button" className="primary-button audit-export-button" onClick={exportLogs} disabled={!visibleEvents.length}>Descargar log.txt</button>
            </div>
          </div>
          <div className="detail-pagination" aria-label="Paginación de auditoría">
            <label className="detail-page-size">
              <span>Mostrar</span>
              <select
                value={pageSize}
                onChange={(event) => {
                  const nextSize = Number(event.target.value) as 25 | 50;
                  setPageSize(nextSize);
                  setPage(0);
                  void loadLogs(false, 0, nextSize);
                }}
              >
                <option value="25">25</option>
                <option value="50">50</option>
              </select>
              <span>por página</span>
            </label>
            <span className="detail-pagination__range">
              Mostrando {totalElements === 0 ? 0 : (page * pageSize) + 1}–{Math.min((page + 1) * pageSize, totalElements)} de {totalElements} registros
            </span>
            <div className="detail-pagination__controls">
              <button
                type="button"
                className="secondary-button"
                disabled={page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                Anterior
              </button>
              <span>Página {page + 1} de {totalPages}</span>
              <button
                type="button"
                className="secondary-button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                Siguiente
              </button>
            </div>
          </div>
          <div className="table-wrapper">
            <table className="audit-table"><thead><tr><th>Fecha y hora</th><th>Severidad</th><th>Evento</th><th>Archivo</th><th>Detalle</th></tr></thead><tbody>
              {visibleEvents.map((event) => <tr key={event.id}><td className="audit-timestamp"><time dateTime={event.createdAt}>{formatTimestamp(event.createdAt)}</time></td><td><span className={`audit-severity audit-severity--${event.level.toLowerCase()}`}><span aria-hidden="true" />{severityLabels[event.level]}</span></td><td><strong className="audit-event">{event.event}</strong></td><td><span className="file-name audit-file-name">{event.fileName ?? "No asociado"}</span></td><td><span className="audit-message">{event.message}</span></td></tr>)}
            </tbody></table>
            {!visibleEvents.length ? <div className="empty-state"><strong>No se encontraron eventos</strong><span>Ajuste la búsqueda o espere actividad del proceso.</span></div> : null}
          </div>
        </section>
      </main>
    </div>
  );
}
