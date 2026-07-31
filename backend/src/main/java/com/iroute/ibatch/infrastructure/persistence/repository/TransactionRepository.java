package com.iroute.ibatch.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.TransactionRejection;

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
}
