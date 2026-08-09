package com.iroute.ibatch.infrastructure.persistence.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.FileTransactionCounters;
import com.iroute.ibatch.domain.model.PersistedTransactionRejection;
import com.iroute.ibatch.domain.model.ProcessedTransaction;
import com.iroute.ibatch.domain.model.RejectionReasonSummary;
import com.iroute.ibatch.domain.model.TransactionRejection;
import com.iroute.ibatch.domain.model.TransactionRejectionDetail;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TransactionRepository(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public boolean existsProcessedUniqueKey(String processedUniqueKey) {
        var sql = """
                SELECT COUNT(1)
                FROM transactions
                WHERE processed_unique_key = ?
                """;
        var count = jdbcTemplate.queryForObject(sql, Integer.class, processedUniqueKey);

        return count != null && count > 0;
    }

    public boolean existsProcessedUniqueKeyExcludingTransaction(String processedUniqueKey, Long transactionId) {
        var sql = """
                SELECT COUNT(1)
                FROM transactions
                WHERE processed_unique_key = ?
                  AND transaction_id <> ?
                """;
        var count = jdbcTemplate.queryForObject(sql, Integer.class, processedUniqueKey, transactionId);

        return count != null && count > 0;
    }

    public Long save(Long fileId, CsvTransactionRow row) {
        var sql = """
                INSERT INTO transactions (
                    file_id,
                    line_number,
                    raw_account,
                    raw_amount,
                    raw_date,
                    account,
                    amount,
                    transaction_date,
                    transaction_status_id,
                    processed_unique_key,
                    record_status_id
                ) VALUES (
                    :fileId,
                    :lineNumber,
                    :rawAccount,
                    :rawAmount,
                    :rawDate,
                    :account,
                    :amount,
                    :transactionDate,
                    :transactionStatusId,
                    :processedUniqueKey,
                    1
                )
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("fileId", fileId)
                .addValue("lineNumber", row.lineNumber())
                .addValue("rawAccount", row.rawAccount())
                .addValue("rawAmount", row.rawAmount())
                .addValue("rawDate", row.rawDate())
                .addValue("account", row.account())
                .addValue("amount", row.amount())
                .addValue("transactionDate", row.transactionDate())
                .addValue("transactionStatusId", row.transactionStatusId())
                .addValue("processedUniqueKey", row.processedUniqueKey());
        var keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, parameters, keyHolder, new String[] {"transaction_id"});

        return getGeneratedId(keyHolder);
    }

    public void saveBatch(Long fileId, List<CsvTransactionRow> rows, boolean ignoreDuplicates) {
        if (rows.isEmpty()) {
            return;
        }

        var sql = """
                INSERT %s INTO transactions (
                    file_id,
                    line_number,
                    raw_account,
                    raw_amount,
                    raw_date,
                    account,
                    amount,
                    transaction_date,
                    transaction_status_id,
                    processed_unique_key,
                    record_status_id
                ) VALUES (
                    :fileId,
                    :lineNumber,
                    :rawAccount,
                    :rawAmount,
                    :rawDate,
                    :account,
                    :amount,
                    :transactionDate,
                    :transactionStatusId,
                    :processedUniqueKey,
                    1
                )
                """.formatted(ignoreDuplicates ? "IGNORE" : "");
        SqlParameterSource[] parameters = rows.stream()
                .map(row -> new MapSqlParameterSource()
                        .addValue("fileId", fileId)
                        .addValue("lineNumber", row.lineNumber())
                        .addValue("rawAccount", row.rawAccount())
                        .addValue("rawAmount", row.rawAmount())
                        .addValue("rawDate", row.rawDate())
                        .addValue("account", row.account())
                        .addValue("amount", row.amount())
                        .addValue("transactionDate", row.transactionDate())
                        .addValue("transactionStatusId", row.transactionStatusId())
                        .addValue("processedUniqueKey", row.processedUniqueKey()))
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, parameters);
    }

    public Set<String> findExistingProcessedUniqueKeys(Set<String> uniqueKeys) {
        if (uniqueKeys.isEmpty()) {
            return Set.of();
        }

        var sql = """
                SELECT processed_unique_key
                FROM transactions
                WHERE processed_unique_key IN (:uniqueKeys)
                """;
        var parameters = new MapSqlParameterSource("uniqueKeys", uniqueKeys);

        return Set.copyOf(namedParameterJdbcTemplate.queryForList(sql, parameters, String.class));
    }

    public Map<Integer, Long> findIdsByFileAndLineNumbers(Long fileId, List<Integer> lineNumbers) {
        if (lineNumbers.isEmpty()) {
            return Map.of();
        }

        var sql = """
                SELECT transaction_id, line_number
                FROM transactions
                WHERE file_id = :fileId
                  AND line_number IN (:lineNumbers)
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("fileId", fileId)
                .addValue("lineNumbers", lineNumbers);
        var result = new HashMap<Integer, Long>();

        namedParameterJdbcTemplate.query(sql, parameters, resultSet -> {
            result.put(resultSet.getInt("line_number"), resultSet.getLong("transaction_id"));
        });

        return result;
    }

    public List<ProcessedTransaction> findByFileId(Long fileId) {
        var sql = """
                SELECT t.transaction_id,
                       t.file_id,
                       t.line_number,
                       t.raw_account,
                       t.raw_amount,
                       t.raw_date,
                       t.account,
                       t.amount,
                       t.transaction_date,
                       ts.code AS status,
                       t.created_at,
                       t.updated_at
                FROM transactions t
                INNER JOIN transaction_status ts
                        ON ts.transaction_status_id = t.transaction_status_id
                WHERE t.file_id = ?
                ORDER BY t.line_number ASC, t.transaction_id ASC
                """;

        return jdbcTemplate.query(sql, this::mapTransactionRow, fileId);
    }

    public List<ProcessedTransaction> findByFileId(
            Long fileId,
            int page,
            int size,
            String status,
            String account) {
        var sql = """
                SELECT t.transaction_id,
                       t.file_id,
                       t.line_number,
                       t.raw_account,
                       t.raw_amount,
                       t.raw_date,
                       t.account,
                       t.amount,
                       t.transaction_date,
                       ts.code AS status,
                       t.created_at,
                       t.updated_at
                FROM transactions t
                INNER JOIN transaction_status ts
                        ON ts.transaction_status_id = t.transaction_status_id
                WHERE t.file_id = :fileId
                  AND (:status IS NULL OR ts.code = :status)
                  AND (:account IS NULL OR LOCATE(:account, COALESCE(t.account, t.raw_account, '')) > 0)
                ORDER BY t.line_number ASC, t.transaction_id ASC
                LIMIT :size OFFSET :offset
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("fileId", fileId)
                .addValue("status", status)
                .addValue("account", account)
                .addValue("size", size)
                .addValue("offset", page * size);

        return namedParameterJdbcTemplate.query(sql, parameters, this::mapTransactionRow);
    }

    public long countByFileId(Long fileId, String status, String account) {
        var sql = """
                SELECT COUNT(*)
                FROM transactions t
                INNER JOIN transaction_status ts
                        ON ts.transaction_status_id = t.transaction_status_id
                WHERE t.file_id = :fileId
                  AND (:status IS NULL OR ts.code = :status)
                  AND (:account IS NULL OR LOCATE(:account, COALESCE(t.account, t.raw_account, '')) > 0)
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("fileId", fileId)
                .addValue("status", status)
                .addValue("account", account);
        var count = namedParameterJdbcTemplate.queryForObject(sql, parameters, Long.class);

        return count == null ? 0 : count;
    }

    public Optional<ProcessedTransaction> findById(Long transactionId) {
        var sql = """
                SELECT t.transaction_id,
                       t.file_id,
                       t.line_number,
                       t.raw_account,
                       t.raw_amount,
                       t.raw_date,
                       t.account,
                       t.amount,
                       t.transaction_date,
                       ts.code AS status,
                       t.created_at,
                       t.updated_at
                FROM transactions t
                INNER JOIN transaction_status ts
                        ON ts.transaction_status_id = t.transaction_status_id
                WHERE t.transaction_id = ?
                """;
        var transactions = jdbcTemplate.query(sql, this::mapTransactionRow, transactionId);

        return transactions.stream().findFirst();
    }

    public List<TransactionRejectionDetail> findRejectionsByFileId(Long fileId) {
        var sql = """
                SELECT tr.transaction_rejection_id,
                       tr.transaction_id,
                       rr.code AS reason_code,
                       rr.name AS reason_name,
                       tr.message,
                       tr.created_at
                FROM transaction_rejections tr
                INNER JOIN transactions t
                        ON t.transaction_id = tr.transaction_id
                INNER JOIN rejection_reason rr
                        ON rr.rejection_reason_id = tr.rejection_reason_id
                WHERE t.file_id = ?
                ORDER BY tr.transaction_rejection_id ASC
                """;

        return jdbcTemplate.query(sql, this::mapRejectionRow, fileId);
    }

    public List<TransactionRejectionDetail> findRejectionsByTransactionId(Long transactionId) {
        var sql = """
                SELECT tr.transaction_rejection_id,
                       tr.transaction_id,
                       rr.code AS reason_code,
                       rr.name AS reason_name,
                       tr.message,
                       tr.created_at
                FROM transaction_rejections tr
                INNER JOIN rejection_reason rr
                        ON rr.rejection_reason_id = tr.rejection_reason_id
                WHERE tr.transaction_id = ?
                ORDER BY tr.transaction_rejection_id ASC
                """;

        return jdbcTemplate.query(sql, this::mapRejectionRow, transactionId);
    }

    public List<TransactionRejectionDetail> findRejectionsByTransactionIds(List<Long> transactionIds) {
        if (transactionIds.isEmpty()) {
            return List.of();
        }

        var sql = """
                SELECT tr.transaction_rejection_id,
                       tr.transaction_id,
                       rr.code AS reason_code,
                       rr.name AS reason_name,
                       tr.message,
                       tr.created_at
                FROM transaction_rejections tr
                INNER JOIN rejection_reason rr
                        ON rr.rejection_reason_id = tr.rejection_reason_id
                WHERE tr.transaction_id IN (:transactionIds)
                ORDER BY tr.transaction_rejection_id ASC
                """;
        var parameters = new MapSqlParameterSource("transactionIds", transactionIds);

        return namedParameterJdbcTemplate.query(sql, parameters, this::mapRejectionRow);
    }

    public void updateReprocessedAmount(
            Long transactionId,
            BigDecimal amount,
            int transactionStatusId,
            String processedUniqueKey) {
        var sql = """
                UPDATE transactions
                SET raw_amount = :rawAmount,
                    amount = :amount,
                    transaction_status_id = :transactionStatusId,
                    processed_unique_key = :processedUniqueKey
                WHERE transaction_id = :transactionId
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("transactionId", transactionId)
                .addValue("rawAmount", amount.toPlainString())
                .addValue("amount", amount)
                .addValue("transactionStatusId", transactionStatusId)
                .addValue("processedUniqueKey", processedUniqueKey);

        namedParameterJdbcTemplate.update(sql, parameters);
    }

    public void deleteRejections(Long transactionId) {
        var sql = """
                DELETE FROM transaction_rejections
                WHERE transaction_id = ?
                """;

        jdbcTemplate.update(sql, transactionId);
    }

    public void saveRejections(Long transactionId, List<TransactionRejection> rejections) {
        var sql = """
                INSERT INTO transaction_rejections (
                    transaction_id,
                    rejection_reason_id,
                    message
                ) VALUES (
                    :transactionId,
                    :rejectionReasonId,
                    :message
                )
                """;

        for (var rejection : rejections) {
            var parameters = new MapSqlParameterSource()
                    .addValue("transactionId", transactionId)
                    .addValue("rejectionReasonId", rejection.rejectionReasonId())
                    .addValue("message", rejection.message());

            namedParameterJdbcTemplate.update(sql, parameters);
        }
    }

    public void saveRejectionsBatch(List<PersistedTransactionRejection> rejections) {
        if (rejections.isEmpty()) {
            return;
        }

        var sql = """
                INSERT INTO transaction_rejections (
                    transaction_id,
                    rejection_reason_id,
                    message
                ) VALUES (
                    :transactionId,
                    :rejectionReasonId,
                    :message
                )
                """;
        SqlParameterSource[] parameters = rejections.stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("transactionId", item.transactionId())
                        .addValue("rejectionReasonId", item.rejection().rejectionReasonId())
                        .addValue("message", item.rejection().message()))
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, parameters);
    }

    public void saveReprocessHistory(
            ProcessedTransaction transaction,
            BigDecimal newAmount,
            int newStatusId,
            String previousRejectionSummary,
            String newRejectionSummary) {
        var sql = """
                INSERT INTO transaction_reprocess_history (
                    transaction_id,
                    previous_amount,
                    new_amount,
                    previous_status_id,
                    new_status_id,
                    previous_rejection_summary,
                    new_rejection_summary
                ) VALUES (
                    :transactionId,
                    :previousAmount,
                    :newAmount,
                    :previousStatusId,
                    :newStatusId,
                    :previousRejectionSummary,
                    :newRejectionSummary
                )
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("transactionId", transaction.transactionId())
                .addValue("previousAmount", transaction.amount())
                .addValue("newAmount", newAmount)
                .addValue("previousStatusId", toStatusId(transaction.status()))
                .addValue("newStatusId", newStatusId)
                .addValue("previousRejectionSummary", previousRejectionSummary)
                .addValue("newRejectionSummary", newRejectionSummary);

        namedParameterJdbcTemplate.update(sql, parameters);
    }

    public FileTransactionCounters countByFileId(Long fileId) {
        var sql = """
                SELECT COUNT(1) AS total_records,
                       SUM(CASE WHEN transaction_status_id = 1 THEN 1 ELSE 0 END) AS processed_count,
                       SUM(CASE WHEN transaction_status_id = 2 THEN 1 ELSE 0 END) AS rejected_count
                FROM transactions
                WHERE file_id = ?
                """;

        return jdbcTemplate.queryForObject(sql, (resultSet, rowNumber) -> new FileTransactionCounters(
                resultSet.getInt("total_records"),
                resultSet.getInt("processed_count"),
                resultSet.getInt("rejected_count")), fileId);
    }

    public List<RejectionReasonSummary> findRejectionReasonSummary() {
        var sql = """
                SELECT rr.code,
                       rr.name,
                       COUNT(*) AS rejection_count
                FROM transaction_rejections tr
                INNER JOIN rejection_reason rr
                        ON rr.rejection_reason_id = tr.rejection_reason_id
                GROUP BY rr.code, rr.name
                ORDER BY rejection_count DESC, rr.code ASC
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new RejectionReasonSummary(
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getInt("rejection_count")));
    }

    private int toStatusId(String status) {
        return "PROCESADO".equals(status) ? 1 : 2;
    }

    private ProcessedTransaction mapTransactionRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProcessedTransaction(
                resultSet.getLong("transaction_id"),
                resultSet.getLong("file_id"),
                resultSet.getInt("line_number"),
                resultSet.getString("raw_account"),
                resultSet.getString("raw_amount"),
                resultSet.getString("raw_date"),
                resultSet.getString("account"),
                resultSet.getBigDecimal("amount"),
                resultSet.getDate("transaction_date") == null
                        ? null
                        : resultSet.getDate("transaction_date").toLocalDate(),
                resultSet.getString("status"),
                toLocalDateTime(resultSet.getTimestamp("created_at")),
                toLocalDateTime(resultSet.getTimestamp("updated_at")));
    }

    private TransactionRejectionDetail mapRejectionRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TransactionRejectionDetail(
                resultSet.getLong("transaction_rejection_id"),
                resultSet.getLong("transaction_id"),
                resultSet.getString("reason_code"),
                resultSet.getString("reason_name"),
                resultSet.getString("message"),
                toLocalDateTime(resultSet.getTimestamp("created_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long getGeneratedId(GeneratedKeyHolder keyHolder) {
        var key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException("No se obtuvo el identificador generado");
        }

        return key.longValue();
    }
}
