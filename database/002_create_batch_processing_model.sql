USE ibatch;

CREATE TABLE IF NOT EXISTS record_status (
  record_status_id INT NOT NULL,
  code VARCHAR(30) NOT NULL,
  name VARCHAR(60) NOT NULL,
  PRIMARY KEY (record_status_id),
  UNIQUE KEY uk_record_status_code (code)
);

CREATE TABLE IF NOT EXISTS file_status (
  file_status_id INT NOT NULL,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(80) NOT NULL,
  PRIMARY KEY (file_status_id),
  UNIQUE KEY uk_file_status_code (code)
);

CREATE TABLE IF NOT EXISTS transaction_status (
  transaction_status_id INT NOT NULL,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(80) NOT NULL,
  PRIMARY KEY (transaction_status_id),
  UNIQUE KEY uk_transaction_status_code (code)
);

CREATE TABLE IF NOT EXISTS rejection_reason (
  rejection_reason_id INT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(120) NOT NULL,
  PRIMARY KEY (rejection_reason_id),
  UNIQUE KEY uk_rejection_reason_code (code)
);

CREATE TABLE IF NOT EXISTS log_level (
  log_level_id INT NOT NULL,
  code VARCHAR(30) NOT NULL,
  name VARCHAR(60) NOT NULL,
  PRIMARY KEY (log_level_id),
  UNIQUE KEY uk_log_level_code (code)
);

CREATE TABLE IF NOT EXISTS log_event_type (
  log_event_type_id INT NOT NULL,
  code VARCHAR(60) NOT NULL,
  name VARCHAR(120) NOT NULL,
  PRIMARY KEY (log_event_type_id),
  UNIQUE KEY uk_log_event_type_code (code)
);

CREATE TABLE IF NOT EXISTS files (
  file_id BIGINT NOT NULL AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  original_path VARCHAR(1000) NULL,
  processed_path VARCHAR(1000) NULL,
  file_date DATE NULL,
  file_status_id INT NOT NULL,
  total_records INT NOT NULL DEFAULT 0,
  processed_count INT NOT NULL DEFAULT 0,
  rejected_count INT NOT NULL DEFAULT 0,
  started_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL,
  record_status_id INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (file_id),
  UNIQUE KEY uk_files_file_name (file_name),
  KEY idx_files_file_status_id (file_status_id),
  KEY idx_files_record_status_id (record_status_id),
  KEY idx_files_created (created_at, file_id),
  CONSTRAINT fk_files_file_status
    FOREIGN KEY (file_status_id) REFERENCES file_status (file_status_id),
  CONSTRAINT fk_files_record_status
    FOREIGN KEY (record_status_id) REFERENCES record_status (record_status_id)
);

CREATE TABLE IF NOT EXISTS transactions (
  transaction_id BIGINT NOT NULL AUTO_INCREMENT,
  file_id BIGINT NOT NULL,
  line_number INT NOT NULL,
  raw_account VARCHAR(100) NULL,
  raw_amount VARCHAR(100) NULL,
  raw_date VARCHAR(100) NULL,
  account VARCHAR(10) NULL,
  amount DECIMAL(18, 2) NULL,
  transaction_date DATE NULL,
  transaction_status_id INT NOT NULL,
  processed_unique_key VARCHAR(255) NULL,
  record_status_id INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (transaction_id),
  UNIQUE KEY uk_transactions_processed_unique_key (processed_unique_key),
  KEY idx_transactions_file_id (file_id),
  KEY idx_transactions_file_line (file_id, line_number),
  KEY idx_transactions_file_status (file_id, transaction_status_id),
  KEY idx_transactions_transaction_status_id (transaction_status_id),
  KEY idx_transactions_record_status_id (record_status_id),
  CONSTRAINT fk_transactions_file
    FOREIGN KEY (file_id) REFERENCES files (file_id),
  CONSTRAINT fk_transactions_transaction_status
    FOREIGN KEY (transaction_status_id) REFERENCES transaction_status (transaction_status_id),
  CONSTRAINT fk_transactions_record_status
    FOREIGN KEY (record_status_id) REFERENCES record_status (record_status_id)
);

CREATE TABLE IF NOT EXISTS transaction_rejections (
  transaction_rejection_id BIGINT NOT NULL AUTO_INCREMENT,
  transaction_id BIGINT NOT NULL,
  rejection_reason_id INT NOT NULL,
  message VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transaction_rejection_id),
  KEY idx_transaction_rejections_transaction_id (transaction_id),
  KEY idx_transaction_rejections_rejection_reason_id (rejection_reason_id),
  CONSTRAINT fk_transaction_rejections_transaction
    FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id),
  CONSTRAINT fk_transaction_rejections_rejection_reason
    FOREIGN KEY (rejection_reason_id) REFERENCES rejection_reason (rejection_reason_id)
);

CREATE TABLE IF NOT EXISTS transaction_reprocess_history (
  reprocess_id BIGINT NOT NULL AUTO_INCREMENT,
  transaction_id BIGINT NOT NULL,
  previous_amount DECIMAL(18, 2) NULL,
  new_amount DECIMAL(18, 2) NOT NULL,
  previous_status_id INT NOT NULL,
  new_status_id INT NOT NULL,
  previous_rejection_summary VARCHAR(1000) NULL,
  new_rejection_summary VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (reprocess_id),
  KEY idx_transaction_reprocess_history_transaction_id (transaction_id),
  CONSTRAINT fk_transaction_reprocess_history_transaction
    FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id),
  CONSTRAINT fk_transaction_reprocess_history_previous_status
    FOREIGN KEY (previous_status_id) REFERENCES transaction_status (transaction_status_id),
  CONSTRAINT fk_transaction_reprocess_history_new_status
    FOREIGN KEY (new_status_id) REFERENCES transaction_status (transaction_status_id)
);

CREATE TABLE IF NOT EXISTS processing_logs (
  log_id BIGINT NOT NULL AUTO_INCREMENT,
  file_id BIGINT NULL,
  transaction_id BIGINT NULL,
  log_level_id INT NOT NULL,
  log_event_type_id INT NOT NULL,
  message VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_processing_logs_file_id (file_id),
  KEY idx_processing_logs_transaction_id (transaction_id),
  KEY idx_processing_logs_log_level_id (log_level_id),
  KEY idx_processing_logs_log_event_type_id (log_event_type_id),
  KEY idx_processing_logs_created (created_at, log_id),
  CONSTRAINT fk_processing_logs_file
    FOREIGN KEY (file_id) REFERENCES files (file_id),
  CONSTRAINT fk_processing_logs_transaction
    FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id),
  CONSTRAINT fk_processing_logs_log_level
    FOREIGN KEY (log_level_id) REFERENCES log_level (log_level_id),
  CONSTRAINT fk_processing_logs_log_event_type
    FOREIGN KEY (log_event_type_id) REFERENCES log_event_type (log_event_type_id)
);

INSERT INTO record_status (record_status_id, code, name) VALUES
  (1, 'ACTIVO', 'Activo'),
  (2, 'INACTIVO', 'Inactivo')
ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name);

INSERT INTO file_status (file_status_id, code, name) VALUES
  (1, 'DISPONIBLE', 'Disponible'),
  (2, 'PROCESANDO', 'Procesando'),
  (3, 'PROCESADO', 'Procesado'),
  (4, 'PROCESADO_CON_RECHAZOS', 'Procesado con rechazos'),
  (5, 'ERROR', 'Error')
ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name);

INSERT INTO transaction_status (transaction_status_id, code, name) VALUES
  (1, 'PROCESADO', 'Procesado'),
  (2, 'RECHAZADA', 'Rechazada')
ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name);

INSERT INTO rejection_reason (rejection_reason_id, code, name) VALUES
  (1, 'CUENTA_VACIA', 'Cuenta vacia'),
  (2, 'CUENTA_INVALIDA', 'Cuenta invalida'),
  (3, 'MONTO_VACIO', 'Monto vacio'),
  (4, 'MONTO_INVALIDO', 'Monto invalido'),
  (5, 'FECHA_VACIA', 'Fecha vacia'),
  (6, 'FECHA_INVALIDA', 'Fecha invalida'),
  (7, 'DUPLICADO', 'Duplicado'),
  (8, 'FILA_CORRUPTA', 'Fila corrupta'),
  (9, 'ESTRUCTURA_INVALIDA', 'Estructura invalida')
ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name);

INSERT INTO log_level (log_level_id, code, name) VALUES
  (1, 'INFO', 'Informacion'),
  (2, 'SUCCESS', 'Exito'),
  (3, 'WARNING', 'Advertencia'),
  (4, 'ERROR', 'Error')
ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name);

INSERT INTO log_event_type (log_event_type_id, code, name) VALUES
  (1, 'FILE_DETECTED', 'Archivo detectado'),
  (2, 'FILE_VALIDATION_FAILED', 'Validacion de archivo fallida'),
  (3, 'FILE_PROCESS_STARTED', 'Procesamiento de archivo iniciado'),
  (4, 'ROW_PROCESSED', 'Fila procesada'),
  (5, 'ROW_REJECTED', 'Fila rechazada'),
  (6, 'FILE_PROCESS_FINISHED', 'Procesamiento de archivo finalizado'),
  (7, 'TRANSACTION_REPROCESS_STARTED', 'Reproceso de transaccion iniciado'),
  (8, 'TRANSACTION_REPROCESS_FINISHED', 'Reproceso de transaccion finalizado'),
  (9, 'PROCESS_ERROR', 'Error de procesamiento')
ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name);
