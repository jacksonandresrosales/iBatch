package com.iroute.ibatch.infrastructure.persistence.repository;

import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.ProcessedTransaction;
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

        return keyHolder.getKeyAs(Long.class);
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
}
