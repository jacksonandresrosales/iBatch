USE ibatch;

CREATE TABLE IF NOT EXISTS processed_files (
  id BIGINT NOT NULL AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL,
  total_transactions INT NOT NULL DEFAULT 0,
  processed_transactions INT NOT NULL DEFAULT 0,
  rejected_transactions INT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_processed_files_file_name (file_name)
);
