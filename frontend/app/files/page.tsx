"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import AppHeader from "../components/AppHeader";
import {
  getFileDetail,
  getProcessedFiles,
  reprocessTransaction as reprocessTransactionRequest,
  type FileDetailResponse,
  type ProcessedFileResponse,
} from "../../lib/api";

type HistoryStatus = "PROCESANDO" | "PROCESADO" | "PROCESADO_CON_RECHAZOS" | "ERROR";

type RejectedTransaction = {
  id: string;
  account: string;
  date: string;
  amount: number;
  reason: string;
};

type FileTransaction = {
  id: string;
  account: string;
  date: string;
  amount: number;
  status: "PROCESADO" | "RECHAZADA";
  rejectionReason?: string;
};

type ProcessedFile = {
  id: string;
  name: string;
  batchDate: string;
  processedAt: string;
  total: number;
  processed: number;
  rejected: number;
  status: HistoryStatus;
  rejectedTransactions: RejectedTransaction[];
  transactions: FileTransaction[];
  detailTotalElements: number;
  detailTotalPages: number;
  errorDetail?: string;
};

function formatFileDate(fileName: string) {
  const datePart = fileName.match(/transactions_(\d{2})(\d{2})(\d{4})\.csv/i);
  if (!datePart) return "Fecha no disponible";

  return new Intl.DateTimeFormat("es-EC", { day: "2-digit", month: "short", year: "numeric" })
    .format(new Date(Number(datePart[3]), Number(datePart[2]) - 1, Number(datePart[1])));
}

function formatTimestamp(timestamp: string) {
  return new Intl.DateTimeFormat("es-EC", {
    dateStyle: "medium",
    timeStyle: "short",
    hour12: false,
  }).format(new Date(timestamp));
}

function mapProcessedFile(file: ProcessedFileResponse): ProcessedFile {
  return {
    id: String(file.id),
    name: file.fileName,
    batchDate: formatFileDate(file.fileName),
    processedAt: formatTimestamp(file.updatedAt),
    total: file.totalTransactions,
    processed: file.processedTransactions,
    rejected: file.rejectedTransactions,
    status: file.status,
    rejectedTransactions: [],
    transactions: [],
    detailTotalElements: 0,
    detailTotalPages: 0,
    errorDetail: file.errorMessage ?? undefined,
  };
}

function mapFileDetail(detail: FileDetailResponse): ProcessedFile {
  const file = mapProcessedFile(detail.file);
  const transactions = detail.transactions.map((transaction) => ({
    id: String(transaction.transactionId),
    account: transaction.account ?? transaction.rawAccount ?? "No disponible",
    date: transaction.transactionDate ?? transaction.rawDate ?? "No disponible",
    amount: transaction.amount ?? 0,
    status: transaction.status,
    rejectionReason: transaction.rejections[0]?.reasonName ?? transaction.rejections[0]?.reasonCode,
  }));

  return {
    ...file,
    transactions,
    detailTotalElements: detail.totalElements,
    detailTotalPages: detail.totalPages,
    rejectedTransactions: transactions
      .filter((transaction) => transaction.status === "RECHAZADA")
      .map((transaction) => ({
        id: transaction.id,
        account: transaction.account,
        date: transaction.date,
        amount: transaction.amount,
        reason: transaction.rejectionReason ?? "Rechazada",
      })),
  };
}

const numberFormat = new Intl.NumberFormat("es-EC");

function statusLabel(status: HistoryStatus) {
  if (status === "PROCESANDO") return "Procesando";
  if (status === "PROCESADO_CON_RECHAZOS") return "Con rechazos";
  if (status === "ERROR") return "Error";
  return "Procesado";
}

function canReprocess(reason: string) {
  return Boolean(reason);
}

export default function ProcessedFilesPage() {
  const [files, setFiles] = useState<ProcessedFile[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"TODOS" | HistoryStatus>(
    "TODOS",
  );
  const [notice, setNotice] = useState<string | null>(null);
  const [isReprocessOpen, setIsReprocessOpen] = useState(false);
  const [selectedRejectedId, setSelectedRejectedId] = useState("");
  const [replacementAmount, setReplacementAmount] = useState("");
  const [isReprocessing, setIsReprocessing] = useState(false);
  const [detailFileId, setDetailFileId] = useState<string | null>(null);
  const [detailTab, setDetailTab] = useState<"summary" | "transactions">(
    "transactions",
  );
  const [transactionPage, setTransactionPage] = useState(1);
  const [transactionPageSize, setTransactionPageSize] = useState<25 | 50>(25);
  const [transactionStatusFilter, setTransactionStatusFilter] = useState<"TODOS" | "PROCESADO" | "RECHAZADA">("TODOS");
  const [transactionAccountQuery, setTransactionAccountQuery] = useState("");
  const [appliedTransactionAccountQuery, setAppliedTransactionAccountQuery] = useState("");

  const selectedFile = files.find((file) => file.id === selectedId) ?? null;
  const detailFile = files.find((file) => file.id === detailFileId) ?? null;
  const selectedRejectedTransaction = selectedFile?.rejectedTransactions.find(
    (transaction) => transaction.id === selectedRejectedId,
  ) ?? null;
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
  const detailTransactions = detailFile?.transactions ?? [];
  const detailPageCount = Math.max(1, detailFile?.detailTotalPages ?? 1);
  const currentTransactionPage = Math.min(transactionPage, detailPageCount);
  const detailReasonCounts = detailFile
    ? Object.entries(
        detailFile.rejectedTransactions.reduce<Record<string, number>>(
          (counts, transaction) => {
            counts[transaction.reason] = (counts[transaction.reason] ?? 0) + 1;
            return counts;
          },
          {},
        ),
      )
    : [];

  const refreshHistory = async (showNotice = true) => {
    try {
      const processedFiles = (await getProcessedFiles()).map(mapProcessedFile);
      setFiles(processedFiles);
      setSelectedId((currentId) =>
        processedFiles.some((file) => file.id === currentId)
          ? currentId
          : (processedFiles[0]?.id ?? ""),
      );
      if (showNotice) {
        setNotice("Historial actualizado.");
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo consultar el historial.");
    }
  };

  useEffect(() => {
    void Promise.resolve().then(() => refreshHistory(false));

    const interval = setInterval(() => {
      void getProcessedFiles().then(data => {
        setFiles(currentFiles => {
          const newFiles = data.map(mapProcessedFile);
          return newFiles.map(newFile => {
            const existing = currentFiles.find(f => f.id === newFile.id);
            if (existing) {
              return {
                ...newFile,
                transactions: existing.transactions,
                detailTotalElements: existing.detailTotalElements,
                detailTotalPages: existing.detailTotalPages,
                rejectedTransactions: existing.rejectedTransactions,
              };
            }
            return newFile;
          });
        });
      }).catch(console.error);
    }, 3000);

    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (detailFileId) {
      setTimeout(() => {
        document.getElementById("file-detail-panel")?.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 100);
    }
  }, [detailFileId]);

  const loadDetailPage = async (
    fileId: string,
    page: number,
    size: 25 | 50,
    statusFilter: "TODOS" | "PROCESADO" | "RECHAZADA" = transactionStatusFilter,
    accountQuery = appliedTransactionAccountQuery,
  ) => {
    try {
      const apiStatus = statusFilter === "TODOS" ? undefined : statusFilter;
      const detail = mapFileDetail(
        await getFileDetail(Number(fileId), page - 1, size, apiStatus, accountQuery || undefined),
      );
      setFiles((currentFiles) =>
        currentFiles.map((file) => (file.id === fileId ? detail : file)),
      );
      setSelectedId(fileId);
      setDetailFileId(fileId);
      setDetailTab("transactions");
      setTransactionPage(page);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo cargar el detalle del archivo.");
    }
  };

  const openFileDetail = async (fileId: string) => {
    setTransactionStatusFilter("TODOS");
    setTransactionAccountQuery("");
    setAppliedTransactionAccountQuery("");
    await loadDetailPage(fileId, 1, transactionPageSize, "TODOS", "");
  };

  const searchTransactionsByAccount = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!detailFile) return;

    const accountQuery = transactionAccountQuery.trim();
    setAppliedTransactionAccountQuery(accountQuery);
    await loadDetailPage(
      detailFile.id,
      1,
      transactionPageSize,
      transactionStatusFilter,
      accountQuery,
    );
  };

  const openReprocessModal = () => {
    const eligibleRejected = selectedFile?.rejectedTransactions.filter(
      (transaction) => canReprocess(transaction.reason),
    ) ?? [];
    if (!eligibleRejected.length) return;
    const firstRejected = eligibleRejected[0];
    setSelectedRejectedId(firstRejected.id);
    setReplacementAmount(firstRejected.amount.toFixed(2));
    setNotice(null);
    setIsReprocessOpen(true);
  };

  const selectRejectedTransaction = (transaction: RejectedTransaction) => {
    setSelectedRejectedId(transaction.id);
    setReplacementAmount(transaction.amount.toFixed(2));
  };

  const reprocessTransaction = async () => {
    if (!selectedFile || !selectedRejectedTransaction) return;

    const normalizedAmount = Number(replacementAmount.replace(",", "."));
    if (!Number.isFinite(normalizedAmount) || normalizedAmount <= 0) {
      setNotice("Ingrese un monto válido mayor que cero para reprocesar.");
      return;
    }

    setIsReprocessing(true);
    try {
      const result = await reprocessTransactionRequest(
        Number(selectedRejectedTransaction.id),
        normalizedAmount,
      );
      await refreshHistory(false);
      await loadDetailPage(selectedFile.id, transactionPage, transactionPageSize);
      setIsReprocessOpen(false);
      setNotice(
        result.status === "PROCESADO"
          ? `Transacción reprocesada correctamente en ${selectedFile.name}.`
          : "La transacción fue reprocesada, pero continúa rechazada.",
      );
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo reprocesar la transacción.");
    } finally {
      setIsReprocessing(false);
    }
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
            <strong>{selectedFile?.processedAt ?? "Sin registros"}</strong>
            <button type="button" className="text-button" onClick={() => void refreshHistory()}>
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
                            onClick={() => openFileDetail(file.id)}
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
                {selectedFile.rejectedTransactions.some((transaction) => canReprocess(transaction.reason)) ? (
                  <button
                    type="button"
                    className="secondary-button history-reprocess-button"
                    onClick={openReprocessModal}
                  >
                    Revisar y reprocesar rechazos
                  </button>
                ) : null}
                <button
                  type="button"
                  className="text-button history-open-detail-button"
                  onClick={() => openFileDetail(selectedFile.id)}
                >
                  Ver detalle del archivo
                </button>
              </div>
            ) : (
              <div className="selected-file-card__empty">
                <strong>Ningún registro seleccionado</strong>
                <span>Seleccione un archivo del historial.</span>
              </div>
            )}
          </aside>
        </section>

        {detailFile ? (
          <section id="file-detail-panel" className="file-panel history-detail-panel" aria-label="Detalle del archivo">
            <div className="panel-header history-detail-panel__header">
              <div>
                <p className="eyebrow">Detalle de archivo</p>
                <h2>{detailFile.name}</h2>
                <p>
                  Resultado del lote procesado el {detailFile.batchDate} y sus transacciones asociadas.
                </p>
              </div>
              <button
                type="button"
                className="text-button"
                onClick={() => setDetailFileId(null)}
              >
                Cerrar detalle
              </button>
            </div>

            <div className="detail-tabs" role="tablist" aria-label="Secciones del detalle">
              <button
                type="button"
                role="tab"
                aria-selected={detailTab === "summary"}
                className={detailTab === "summary" ? "is-active" : ""}
                onClick={() => setDetailTab("summary")}
              >
                Resumen del archivo
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={detailTab === "transactions"}
                className={detailTab === "transactions" ? "is-active" : ""}
                onClick={() => setDetailTab("transactions")}
              >
                Transacciones ({numberFormat.format(detailFile.total)})
              </button>
            </div>

            {detailTab === "summary" ? (
              <div className="detail-summary-content">
                <div className="detail-summary-grid">
                  <div className="detail-summary-item">
                    <span>Estado</span>
                    <strong>{statusLabel(detailFile.status)}</strong>
                  </div>
                  <div className="detail-summary-item">
                    <span>Total de registros</span>
                    <strong>{numberFormat.format(detailFile.total)}</strong>
                  </div>
                  <div className="detail-summary-item">
                    <span>Procesadas</span>
                    <strong>{numberFormat.format(detailFile.processed)}</strong>
                  </div>
                  <div className="detail-summary-item">
                    <span>Rechazadas</span>
                    <strong>{numberFormat.format(detailFile.rejected)}</strong>
                  </div>
                  <div className="detail-summary-item">
                    <span>Fecha de procesamiento</span>
                    <strong>{detailFile.processedAt}</strong>
                  </div>
                </div>

                <div className="detail-reasons">
                  <p className="eyebrow">Motivos registrados</p>
                  {detailReasonCounts.length > 0 ? (
                    <div className="detail-reasons__list">
                      {detailReasonCounts.map(([reason, count]) => (
                        <div key={reason}>
                          <span>{reason}</span>
                          <strong>{count}</strong>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="detail-empty-note">No se registraron rechazos para este archivo.</p>
                  )}
                </div>
              </div>
            ) : (
              <div className="detail-transactions-content">
                <div className="detail-transactions__heading">
                  <p>Vista de transacciones del lote. Los rechazos elegibles pueden reprocesarse modificando únicamente el monto.</p>
                  <div className="detail-transaction-filters">
                    <form className="detail-account-search" onSubmit={searchTransactionsByAccount}>
                      <label className="search-field">
                        <span>Buscar por cuenta</span>
                        <input
                          type="search"
                          inputMode="numeric"
                          value={transactionAccountQuery}
                          onChange={(event) => setTransactionAccountQuery(event.target.value)}
                          placeholder="Número de cuenta"
                        />
                      </label>
                      <button type="submit" className="secondary-button">
                        Buscar
                      </button>
                    </form>
                    <label className="filter-field">
                      <span>Estado</span>
                      <select
                        value={transactionStatusFilter}
                        onChange={(event) => {
                          const nextStatus = event.target.value as "TODOS" | "PROCESADO" | "RECHAZADA";
                          setTransactionStatusFilter(nextStatus);
                          void loadDetailPage(detailFile.id, 1, transactionPageSize, nextStatus);
                        }}
                      >
                        <option value="TODOS">Todos</option>
                        <option value="PROCESADO">Procesados</option>
                        <option value="RECHAZADA">Rechazados</option>
                      </select>
                    </label>
                    <span className="detail-transaction-count">
                      {numberFormat.format(detailFile.detailTotalElements)} registros
                    </span>
                  </div>
                </div>
                <div className="detail-pagination" aria-label="Paginación de transacciones">
                  <label className="detail-page-size">
                    <span>Mostrar</span>
                    <select
                      value={transactionPageSize}
                      onChange={(event) => {
                        const nextSize = Number(event.target.value) as 25 | 50;
                        setTransactionPageSize(nextSize);
                        void loadDetailPage(detailFile.id, 1, nextSize);
                      }}
                    >
                      <option value="25">25</option>
                      <option value="50">50</option>
                    </select>
                    <span>por página</span>
                  </label>
                  <span className="detail-pagination__range">
                    Mostrando {detailFile.detailTotalElements === 0 ? 0 : (currentTransactionPage - 1) * transactionPageSize + 1}–{Math.min(currentTransactionPage * transactionPageSize, detailFile.detailTotalElements)} de {detailFile.detailTotalElements} registros
                  </span>
                  <div className="detail-pagination__controls">
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => void loadDetailPage(detailFile.id, Math.max(1, currentTransactionPage - 1), transactionPageSize)}
                      disabled={currentTransactionPage === 1}
                    >
                      Anterior
                    </button>
                    <span>Página {currentTransactionPage} de {detailPageCount}</span>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => void loadDetailPage(detailFile.id, Math.min(detailPageCount, currentTransactionPage + 1), transactionPageSize)}
                      disabled={currentTransactionPage === detailPageCount}
                    >
                      Siguiente
                    </button>
                  </div>
                </div>
                <div className="table-wrapper">
                  <table className="transaction-detail-table">
                    <thead>
                      <tr>
                        <th>Cuenta</th>
                        <th className="number-column">Monto</th>
                        <th>Fecha</th>
                        <th>Estado</th>
                        <th>Motivo de rechazo</th>
                        <th>Acción</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detailTransactions.map((transaction) => {
                        const isRejected = transaction.status === "RECHAZADA";
                        const isReprocessable = Boolean(
                          transaction.rejectionReason && canReprocess(transaction.rejectionReason),
                        );
                        return (
                          <tr key={transaction.id}>
                            <td>
                              <strong className="transaction-account">{transaction.account}</strong>
                            </td>
                            <td className="number-column">
                              {transaction.amount.toFixed(2)}
                            </td>
                            <td>{transaction.date}</td>
                            <td>
                              <span className={`history-status history-status--${isRejected ? "error" : "procesado"}`}>
                                <span aria-hidden="true" />
                                {isRejected ? "Rechazada" : "Procesada"}
                              </span>
                            </td>
                            <td>
                              <span className="transaction-reason">
                                {transaction.rejectionReason ?? "—"}
                              </span>
                            </td>
                            <td>
                              {isRejected && isReprocessable ? (
                                <button
                                  type="button"
                                  className="text-button history-detail-button"
                                  onClick={() => {
                                    const rejected = detailFile.rejectedTransactions.find(
                                      (item) => item.id === transaction.id,
                                    );
                                    if (!rejected) return;
                                    setSelectedId(detailFile.id);
                                    setSelectedRejectedId(rejected.id);
                                    setReplacementAmount(rejected.amount.toFixed(2));
                                    setIsReprocessOpen(true);
                                  }}
                                >
                                  Reprocesar
                                </button>
                              ) : (
                                <span className="transaction-action-note">Sin acción</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                  {detailTransactions.length === 0 ? (
                    <div className="empty-state">
                      <strong>No se encontraron transacciones</strong>
                      <span>Revise el número de cuenta o el filtro de estado.</span>
                    </div>
                  ) : null}
                </div>
              </div>
            )}
          </section>
        ) : null}

        {isReprocessOpen && selectedFile ? (
          <div
            className="modal-backdrop"
            role="presentation"
            onMouseDown={(event) => {
              if (event.target === event.currentTarget && !isReprocessing) {
                setIsReprocessOpen(false);
              }
            }}
          >
            <section
              className="confirmation-modal reprocess-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="reprocess-title"
            >
              <div className="modal-accent" />
              <p className="eyebrow">Reproceso controlado</p>
              <h2 id="reprocess-title">Editar monto rechazado</h2>
              <p>
                Corrija únicamente el monto de la transacción seleccionada y envíela nuevamente a validación.
              </p>

              <div className="reprocess-context">
                <span>Archivo</span>
                <strong>{selectedFile.name}</strong>
              </div>

              <div className="rejected-transaction-list" aria-label="Transacciones rechazadas">
                {selectedFile.rejectedTransactions
                  .filter((transaction) => canReprocess(transaction.reason))
                  .map((transaction) => (
                  <button
                    type="button"
                    key={transaction.id}
                    className={`rejected-transaction-option ${transaction.id === selectedRejectedId ? "is-selected" : ""}`}
                    onClick={() => selectRejectedTransaction(transaction)}
                    disabled={isReprocessing}
                  >
                    <span>
                      <strong>{transaction.account}</strong>
                      <small>{transaction.date} · {transaction.reason}</small>
                    </span>
                    <strong>{transaction.amount.toFixed(2)}</strong>
                  </button>
                ))}
              </div>

              {selectedRejectedTransaction ? (
                <label className="amount-field">
                  <span>Nuevo monto</span>
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={replacementAmount}
                    onChange={(event) => setReplacementAmount(event.target.value)}
                    disabled={isReprocessing}
                  />
                  <small>Ingrese un valor mayor que cero.</small>
                </label>
              ) : null}

              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setIsReprocessOpen(false)}
                  disabled={isReprocessing}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="primary-button"
                  onClick={reprocessTransaction}
                  disabled={isReprocessing || !selectedRejectedTransaction}
                >
                  {isReprocessing ? "Procesando..." : "Confirmar reproceso"}
                </button>
              </div>
            </section>
          </div>
        ) : null}

        <footer className="product-footer">
          <span>iBatch Financial Operations</span>
          <span>Historial operativo y trazabilidad de lotes</span>
        </footer>
      </main>
    </div>
  );
}
