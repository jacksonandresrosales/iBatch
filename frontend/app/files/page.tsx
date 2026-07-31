"use client";

import { useMemo, useState } from "react";
import AppHeader from "../components/AppHeader";

type HistoryStatus = "PROCESADO" | "PROCESADO_CON_RECHAZOS" | "ERROR";

type ProcessedFile = {
  id: string;
  name: string;
  batchDate: string;
  processedAt: string;
  total: number;
  processed: number;
  rejected: number;
  status: HistoryStatus;
  errorDetail?: string;
};

const initialHistory: ProcessedFile[] = [
  {
    id: "batch-30072026",
    name: "transactions_30072026.csv",
    batchDate: "30 jul 2026",
    processedAt: "30 jul 2026 · 09:14",
    total: 9960,
    processed: 9950,
    rejected: 10,
    status: "PROCESADO_CON_RECHAZOS",
  },
  {
    id: "batch-29072026",
    name: "transactions_29072026.csv",
    batchDate: "29 jul 2026",
    processedAt: "29 jul 2026 · 09:02",
    total: 12430,
    processed: 12430,
    rejected: 0,
    status: "PROCESADO",
  },
  {
    id: "batch-28072026",
    name: "transactions_28072026.csv",
    batchDate: "28 jul 2026",
    processedAt: "28 jul 2026 · 08:27",
    total: 0,
    processed: 0,
    rejected: 0,
    status: "ERROR",
    errorDetail: "Encabezado inválido",
  },
  {
    id: "batch-27072026",
    name: "transactions_27072026.csv",
    batchDate: "27 jul 2026",
    processedAt: "27 jul 2026 · 09:41",
    total: 8200,
    processed: 8184,
    rejected: 16,
    status: "PROCESADO_CON_RECHAZOS",
  },
  {
    id: "batch-26072026",
    name: "transactions_26072026.csv",
    batchDate: "26 jul 2026",
    processedAt: "26 jul 2026 · 08:56",
    total: 7650,
    processed: 7650,
    rejected: 0,
    status: "PROCESADO",
  },
];

const numberFormat = new Intl.NumberFormat("es-EC");

function statusLabel(status: HistoryStatus) {
  if (status === "PROCESADO_CON_RECHAZOS") return "Con rechazos";
  if (status === "ERROR") return "Error";
  return "Procesado";
}

export default function ProcessedFilesPage() {
  const [files, setFiles] = useState(initialHistory);
  const [selectedId, setSelectedId] = useState(() => {
    if (typeof window !== "undefined") {
      const requestedId = new URLSearchParams(window.location.search).get("selected");
      if (requestedId && initialHistory.some((file) => file.id === requestedId)) {
        return requestedId;
      }
    }
    return initialHistory[0]?.id ?? "";
  });
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"TODOS" | HistoryStatus>(
    "TODOS",
  );
  const [notice, setNotice] = useState<string | null>(null);

  const selectedFile = files.find((file) => file.id === selectedId) ?? null;
  const visibleFiles = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return files.filter((file) => {
      const matchesQuery = file.name.toLowerCase().includes(normalizedQuery);
      const matchesStatus =
        statusFilter === "TODOS" || file.status === statusFilter;
      return matchesQuery && matchesStatus;
    });
  }, [files, query, statusFilter]);

  const processedTransactions = files.reduce(
    (sum, file) => sum + file.processed,
    0,
  );
  const rejectedTransactions = files.reduce(
    (sum, file) => sum + file.rejected,
    0,
  );
  const totalTransactions = processedTransactions + rejectedTransactions;
  const rejectionRate = totalTransactions
    ? ((rejectedTransactions / totalTransactions) * 100).toFixed(2)
    : "0.00";

  const refreshHistory = () => {
    setFiles([...initialHistory]);
    setNotice("Historial actualizado. Se muestran los últimos registros.");
  };

  return (
    <div className="application-shell">
      <AppHeader active="history" />

      <main>
        <section className="page-intro">
          <div>
            <p className="eyebrow">Operaciones / Control histórico</p>
            <h1>Historial de archivos</h1>
            <p className="page-description">
              Consulte el resultado de cada lote procesado y ubique rápidamente
              los archivos que requieren revisión.
            </p>
          </div>

          <div className="sync-summary">
            <span className="sync-summary__label">Última actualización</span>
            <strong>Hoy, 09:18</strong>
            <button type="button" className="text-button" onClick={refreshHistory}>
              Actualizar historial
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

        <section className="operational-overview history-metrics" aria-label="Resumen histórico">
          <div className="metric">
            <span>Archivos procesados</span>
            <strong>{files.length.toString().padStart(2, "0")}</strong>
          </div>
          <div className="metric">
            <span>Transacciones procesadas</span>
            <strong>{numberFormat.format(processedTransactions)}</strong>
          </div>
          <div className="metric">
            <span>Transacciones rechazadas</span>
            <strong>{numberFormat.format(rejectedTransactions)}</strong>
          </div>
          <div className="metric">
            <span>Porcentaje de rechazo</span>
            <strong>{rejectionRate}%</strong>
          </div>
        </section>

        <section className="workspace history-workspace">
          <div className="file-panel history-panel">
            <div className="panel-header history-panel__header">
              <div>
                <h2>Archivos procesados</h2>
                <p>Resumen de lotes, resultados y excepciones registradas.</p>
              </div>
              <div className="history-filters">
                <label className="search-field">
                  <span>Buscar archivo</span>
                  <input
                    type="search"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Nombre del archivo"
                  />
                </label>
                <label className="filter-field">
                  <span>Estado</span>
                  <select
                    value={statusFilter}
                    onChange={(event) =>
                      setStatusFilter(
                        event.target.value as "TODOS" | HistoryStatus,
                      )
                    }
                  >
                    <option value="TODOS">Todos</option>
                    <option value="PROCESADO">Procesados</option>
                    <option value="PROCESADO_CON_RECHAZOS">Con rechazos</option>
                    <option value="ERROR">Con error</option>
                  </select>
                </label>
              </div>
            </div>

            <div className="table-wrapper">
              <table className="history-table">
                <thead>
                  <tr>
                    <th className="selection-column">
                      <span className="sr-only">Seleccionar</span>
                    </th>
                    <th>Archivo</th>
                    <th>Estado</th>
                    <th className="number-column">Total</th>
                    <th className="number-column">Procesadas</th>
                    <th className="number-column">Rechazadas</th>
                    <th>Procesado</th>
                    <th>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleFiles.map((file) => {
                    const isSelected = file.id === selectedId;
                    return (
                      <tr
                        key={file.id}
                        className={isSelected ? "is-selected" : undefined}
                        onClick={() => setSelectedId(file.id)}
                      >
                        <td className="selection-column">
                          <input
                            type="radio"
                            name="selected-history-file"
                            checked={isSelected}
                            onChange={() => setSelectedId(file.id)}
                            aria-label={`Seleccionar ${file.name}`}
                          />
                        </td>
                        <td>
                          <strong className="file-name">{file.name}</strong>
                          <span className="file-type">Lote del {file.batchDate}</span>
                        </td>
                        <td>
                          <span className={`history-status history-status--${file.status.toLowerCase()}`}>
                            <span aria-hidden="true" />
                            {statusLabel(file.status)}
                          </span>
                          {file.errorDetail ? (
                            <span className="history-error-detail">{file.errorDetail}</span>
                          ) : null}
                        </td>
                        <td className="number-column">
                          {file.total ? numberFormat.format(file.total) : "—"}
                        </td>
                        <td className="number-column">
                          {file.processed ? numberFormat.format(file.processed) : "—"}
                        </td>
                        <td className="number-column">
                          {file.rejected ? numberFormat.format(file.rejected) : "—"}
                        </td>
                        <td>{file.processedAt}</td>
                        <td>
                          <button
                            type="button"
                            className="text-button history-detail-button"
                            onClick={() => setSelectedId(file.id)}
                          >
                            Ver detalle
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {visibleFiles.length === 0 ? (
                <div className="empty-state">
                  <strong>No se encontraron registros</strong>
                  <span>Ajuste la búsqueda o el filtro de estado.</span>
                </div>
              ) : null}
            </div>

            <div className="panel-footer">
              <span>
                {visibleFiles.length} de {files.length} archivos visibles
              </span>
              <span className="history-panel__hint">
                Seleccione un registro para revisar su resumen.
              </span>
            </div>
          </div>

          <aside className="selected-file-card history-selected-card" aria-label="Resumen del archivo seleccionado">
            <p className="eyebrow">Registro seleccionado</p>
            {selectedFile ? (
              <div className="selected-file-card__content">
                <span className={`history-status history-status--${selectedFile.status.toLowerCase()}`}>
                  <span aria-hidden="true" />
                  {statusLabel(selectedFile.status)}
                </span>
                <strong>{selectedFile.name}</strong>
                <span className="selected-file-card__meta">
                  {selectedFile.processedAt}
                </span>
                <div className="history-selected-card__counts">
                  <span>
                    <strong>{numberFormat.format(selectedFile.processed)}</strong>
                    procesadas
                  </span>
                  <span>
                    <strong>{numberFormat.format(selectedFile.rejected)}</strong>
                    rechazadas
                  </span>
                </div>
              </div>
            ) : (
              <div className="selected-file-card__empty">
                <strong>Ningún registro seleccionado</strong>
                <span>Seleccione un archivo del historial.</span>
              </div>
            )}
          </aside>
        </section>

        <footer className="product-footer">
          <span>iBatch Financial Operations</span>
          <span>Historial operativo y trazabilidad de lotes</span>
        </footer>
      </main>
    </div>
  );
}
