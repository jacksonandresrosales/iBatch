USE ibatch;

SET @index_sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'files' AND index_name = 'idx_files_created'
  ),
  'SELECT 1',
  'CREATE INDEX idx_files_created ON files (created_at, file_id)'
);
PREPARE index_statement FROM @index_sql;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'transactions' AND index_name = 'idx_transactions_file_line'
  ),
  'SELECT 1',
  'CREATE INDEX idx_transactions_file_line ON transactions (file_id, line_number)'
);
PREPARE index_statement FROM @index_sql;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'transactions' AND index_name = 'idx_transactions_file_status'
  ),
  'SELECT 1',
  'CREATE INDEX idx_transactions_file_status ON transactions (file_id, transaction_status_id)'
);
PREPARE index_statement FROM @index_sql;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'processing_logs' AND index_name = 'idx_processing_logs_created'
  ),
  'SELECT 1',
  'CREATE INDEX idx_processing_logs_created ON processing_logs (created_at, log_id)'
);
PREPARE index_statement FROM @index_sql;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;
