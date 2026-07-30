"use client";

import { useEffect, useMemo, useState } from "react";

type AvailableFile = {
  id: string;
  name: string;
  batchDate: string;
  detectedAt: string;
  size: string;
};

const initialFiles: AvailableFile[] = [
  {
    id: "batch-30072026",
    name: "transactions_30072026.csv",
    batchDate: "30 jul 2026",
    detectedAt: "08:42",
    size: "1,84 MB",
  },
  {
    id: "batch-29072026",
    name: "transactions_29072026.csv",
    batchDate: "29 jul 2026",
    detectedAt: "07:58",
    size: "2,12 MB",
  },
  {
    id: "batch-28072026",
    name: "transactions_28072026.csv",
    batchDate: "28 jul 2026",
    detectedAt: "08:11",
    size: "1,67 MB",
  },
];

export default function AvailableFilesPage() {
  const [files, setFiles] = useState(initialFiles);
  const [selectedId, setSelectedId] = useState(initialFiles[0]?.id ?? "");
  const [query, setQuery] = useState("");
  const [isConfirming, setIsConfirming] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
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
    setNotice("Directorio actualizado. No se detectaron cambios.");
  };

  const processSelectedFile = () => {
    if (!selectedFile) return;
    setIsProcessing(true);
    window.setTimeout(() => {
      setFiles((current) =>
        current.filter((file) => file.id !== selectedFile.id),
      );
      setSelectedId("");
      setIsProcessing(false);
      setIsConfirming(false);
      setNotice(
        `${selectedFile.name} fue enviado al flujo de procesamiento.`,
      );
    }, 1200);
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
          <span className="nav-link nav-link--disabled">Historial</span>
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
          <div className="metric metric--assurance">
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

              {visibleFiles.length === 0 ? (
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
                disabled={!selectedFile}
                onClick={() => setIsConfirming(true)}
              >
                Procesar archivo seleccionado
              </button>
            </div>
          </div>

          <aside className="control-panel" aria-label="Controles de seguridad">
            <p className="eyebrow">Control operativo</p>
            <h2>Procesamiento protegido desde el origen</h2>
            <p>
              iBatch verifica la identidad del archivo antes de iniciar la
              lectura y mantiene trazabilidad durante todo el ciclo.
            </p>

            <ol className="assurance-list">
              <li>
                <span>01</span>
                <div>
                  <strong>Ruta controlada</strong>
                  <p>
                    El archivo debe permanecer dentro del directorio autorizado.
                  </p>
                </div>
              </li>
              <li>
                <span>02</span>
                <div>
                  <strong>Estructura validada</strong>
                  <p>
                    Nombre, extensión y encabezado se revisan antes de leer
                    filas.
                  </p>
                </div>
              </li>
              <li>
                <span>03</span>
                <div>
                  <strong>Evidencia persistente</strong>
                  <p>
                    Resultados y rechazos quedan registrados para auditoría.
                  </p>
                </div>
              </li>
            </ol>

            <div className="selected-summary">
              <span>Selección actual</span>
              <strong>
                {selectedFile?.name ?? "Ningún archivo seleccionado"}
              </strong>
            </div>
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
