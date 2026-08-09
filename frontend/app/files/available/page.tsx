"use client";

import { useEffect, useMemo, useState } from "react";
import {
  getAvailableFiles,
  getFileProgress,
  processFile,
  type FileProgressResponse,
} from "../../../lib/api";

type AvailableFile = {
  id: string;
  name: string;
  batchDate: string;
  detectedAt: string;
  size: string;
};

function formatFileDate(fileName: string) {
  const match = fileName.match(/^transactions_(\d{2})(\d{2})(\d{4})\.csv$/i);
  return match ? `${match[1]}/${match[2]}/${match[3]}` : "Fecha no disponible";
}

function formatFileSize(sizeBytes: number) {
  return new Intl.NumberFormat("es-EC", {
    style: "unit",
    unit: "megabyte",
    maximumFractionDigits: 2,
  }).format(sizeBytes / 1024 / 1024);
}

function mapAvailableFile(file: Awaited<ReturnType<typeof getAvailableFiles>>[number]): AvailableFile {
  const detectedAt = new Intl.DateTimeFormat("es-EC", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(file.lastModifiedAt));

  return {
    id: file.fileName,
    name: file.fileName,
    batchDate: formatFileDate(file.fileName),
    detectedAt,
    size: formatFileSize(file.sizeBytes),
  };
}

export default function AvailableFilesPage() {
  const [files, setFiles] = useState<AvailableFile[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [query, setQuery] = useState("");
  const [isConfirming, setIsConfirming] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [progress, setProgress] = useState<FileProgressResponse | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [lastSync, setLastSync] = useState("Hoy, 08:45");

  const selectedFile = files.find((file) => file.id === selectedId) ?? null;
  const visibleFiles = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return files;
    return files.filter((file) =>
      file.name.toLowerCase().includes(normalizedQuery),
    );
  }, [files, query]);

  const loadFiles = async (showSuccessNotice = false) => {
    setIsLoading(true);
    try {
      const availableFiles = (await getAvailableFiles()).map(mapAvailableFile);
      setFiles(availableFiles);
      setSelectedId((current) =>
        availableFiles.some((file) => file.id === current)
          ? current
          : availableFiles[0]?.id ?? "",
      );
      if (showSuccessNotice) setNotice("Directorio actualizado correctamente.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo consultar el directorio.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadFiles();
  }, []);

  useEffect(() => {
    if (!progress || progress.completed) return;

    const pollProgress = async () => {
      try {
        const currentProgress = await getFileProgress(progress.fileId);
        setProgress(currentProgress);
        if (currentProgress.completed) {
          setNotice(currentProgress.error
            ? "El procesamiento terminó con error."
            : `El archivo ${currentProgress.fileName} terminó de procesarse.`);
          await loadFiles();
        }
      } catch (error) {
        setNotice(error instanceof Error ? error.message : "No se pudo consultar el progreso.");
      }
    };

    const intervalId = window.setInterval(() => void pollProgress(), 3000);
    void pollProgress();
    return () => window.clearInterval(intervalId);
  }, [progress]);

  useEffect(() => {
    if (!isConfirming) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !isProcessing) setIsConfirming(false);
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [isConfirming, isProcessing]);

  const refreshFiles = () => {
    const time = new Intl.DateTimeFormat("es-EC", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date());
    setLastSync(`Hoy, ${time}`);
    void loadFiles(true);
  };

  const processSelectedFile = async () => {
    if (!selectedFile) return;
    setIsProcessing(true);
    try {
      const response = await processFile(selectedFile.name);
      setIsConfirming(false);
      setSelectedId("");
      setProgress({
        fileId: response.fileId,
        fileName: response.fileName,
        processedCount: 0,
        rejectedCount: 0,
        totalRecords: 0,
        percentage: 0,
        status: "PROCESANDO",
        completed: false,
        error: false,
      });
      setNotice(response.message || `${selectedFile.name} fue enviado al procesamiento.`);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "No se pudo procesar el archivo.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="application-shell">
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
          <a className="nav-link nav-link--active" href="/files/available">
            Operaciones
          </a>
          <a className="nav-link" href="/files">
            Historial
          </a>
          <a className="nav-link" href="/dashboard">
            Dashboard
          </a>
          <a className="nav-link" href="/audit">
            Auditoría
          </a>
        </nav>

        <div className="environment-status" aria-label="Estado del sistema">
          <span className="status-dot" aria-hidden="true" />
          <span>
            <small>Ambiente local</small>
            <strong>Operativo</strong>
          </span>
        </div>
      </header>

      <main>
        <section className="page-intro">
          <div>
            <p className="eyebrow">Operaciones / Procesamiento batch</p>
            <h1>Archivos disponibles</h1>
            <p className="page-description">
              Revise los lotes detectados en el directorio autorizado y
              seleccione el archivo que desea procesar.
            </p>
          </div>

          <div className="sync-summary">
            <span className="sync-summary__label">Última sincronización</span>
            <strong>{lastSync}</strong>
            <button type="button" className="text-button" onClick={refreshFiles}>
              Actualizar directorio
            </button>
          </div>
        </section>

        {notice ? (
          <div className="notice" role="status">
            <span className="notice__line" aria-hidden="true" />
            <span>{notice}</span>
            <button
              type="button"
              onClick={() => setNotice(null)}
              aria-label="Cerrar notificación"
            >
              Cerrar
            </button>
          </div>
        ) : null}

        {progress && !progress.completed ? (
          <div className="notice" role="status">
            <span className="notice__line" aria-hidden="true" />
            <span>
              Procesando {progress.fileName}: {progress.percentage.toFixed(1)}%
              {progress.totalRecords > 0
                ? ` (${progress.processedCount + progress.rejectedCount} de ${progress.totalRecords} filas)`
                : ""}
            </span>
          </div>
        ) : null}

        <section className="operational-overview" aria-label="Resumen operativo">
          <div className="metric">
            <span>Lotes pendientes</span>
            <strong>{files.length.toString().padStart(2, "0")}</strong>
          </div>
          <div className="metric">
            <span>Directorio supervisado</span>
            <strong className="metric__path">/input</strong>
          </div>
          <div className="metric">
            <span>Patrón requerido</span>
            <strong className="metric__path">transactions_DDMMYYYY.csv</strong>
          </div>
          <div className="metric">
            <span>Control de entrada</span>
            <strong>Validación activa</strong>
          </div>
        </section>

        <section className="workspace">
          <div className="file-panel">
            <div className="panel-header">
              <div>
                <h2>Lotes listos para revisión</h2>
                <p>
                  Solo se muestran archivos no procesados con nomenclatura
                  válida.
                </p>
              </div>
              <label className="search-field">
                <span>Buscar archivo</span>
                <input
                  type="search"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Nombre del archivo"
                />
              </label>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th className="selection-column">
                      <span className="sr-only">Seleccionar</span>
                    </th>
                    <th>Archivo</th>
                    <th>Fecha del lote</th>
                    <th>Detección</th>
                    <th>Tamaño</th>
                    <th>Control previo</th>
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
                            name="selected-file"
                            checked={isSelected}
                            onChange={() => setSelectedId(file.id)}
                            aria-label={`Seleccionar ${file.name}`}
                          />
                        </td>
                        <td>
                          <strong className="file-name">{file.name}</strong>
                          <span className="file-type">Archivo CSV</span>
                        </td>
                        <td>{file.batchDate}</td>
                        <td>Hoy, {file.detectedAt}</td>
                        <td>{file.size}</td>
                        <td>
                          <span className="validation-status">
                            <span aria-hidden="true" />
                            Formato validado
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

                  {isLoading ? (
                    <div className="empty-state">
                      <strong>Consultando archivos</strong>
                      <span>Espere mientras se actualiza el directorio.</span>
                    </div>
                  ) : visibleFiles.length === 0 ? (
                    <div className="empty-state">
                  <strong>No se encontraron archivos</strong>
                  <span>
                    Revise el término de búsqueda o actualice el directorio.
                  </span>
                </div>
              ) : null}
            </div>

            <div className="panel-footer">
              <span>
                {visibleFiles.length} de {files.length} archivos visibles
              </span>
              <button
                type="button"
                className="primary-button"
                disabled={!selectedFile || isLoading || isProcessing}
                onClick={() => setIsConfirming(true)}
              >
                Procesar archivo seleccionado
              </button>
            </div>
          </div>

          <aside className="selected-file-card" aria-label="Archivo seleccionado">
            <p className="eyebrow">Selección actual</p>
            {selectedFile ? (
              <div className="selected-file-card__content">
                <span className="selection-state">
                  <span aria-hidden="true" />
                  Listo para procesar
                </span>
                <strong>{selectedFile.name}</strong>
                <span className="selected-file-card__meta">
                  {selectedFile.batchDate} · {selectedFile.size}
                </span>
              </div>
            ) : (
              <div className="selected-file-card__empty">
                <strong>Ningún lote seleccionado</strong>
                <span>Seleccione un archivo del listado.</span>
              </div>
            )}
          </aside>
        </section>

        <footer className="product-footer">
          <span>iBatch Financial Operations</span>
          <span>Procesamiento seguro, controlado y trazable</span>
        </footer>
      </main>

      {isConfirming && selectedFile ? (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isProcessing) {
              setIsConfirming(false);
            }
          }}
        >
          <section
            className="confirmation-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirmation-title"
          >
            <div className="modal-accent" aria-hidden="true" />
            <p className="eyebrow">Confirmación requerida</p>
            <h2 id="confirmation-title">Iniciar procesamiento del lote</h2>
            <p>
              La operación validará cada registro y conservará evidencia de los
              resultados procesados y rechazados.
            </p>

            <dl className="confirmation-details">
              <div>
                <dt>Archivo</dt>
                <dd>{selectedFile.name}</dd>
              </div>
              <div>
                <dt>Fecha del lote</dt>
                <dd>{selectedFile.batchDate}</dd>
              </div>
              <div>
                <dt>Directorio</dt>
                <dd>/input</dd>
              </div>
            </dl>

            <div className="modal-actions">
              <button
                type="button"
                className="secondary-button"
                disabled={isProcessing}
                onClick={() => setIsConfirming(false)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="primary-button"
                disabled={isProcessing}
                onClick={processSelectedFile}
              >
                {isProcessing
                  ? "Iniciando procesamiento..."
                  : "Confirmar y procesar"}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}
