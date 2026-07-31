package com.iroute.ibatch.infrastructure.persistence.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessingLogRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProcessingLogRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public void save(Long fileId, Long transactionId, int logLevelId, int logEventTypeId, String message) {
        var sql = """
                INSERT INTO processing_logs (
                    file_id,
                    transaction_id,
                    log_level_id,
                    log_event_type_id,
                    message
                ) VALUES (
                    :fileId,
                    :transactionId,
                    :logLevelId,
                    :logEventTypeId,
                    :message
                )
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("fileId", fileId)
                .addValue("transactionId", transactionId)
                .addValue("logLevelId", logLevelId)
                .addValue("logEventTypeId", logEventTypeId)
                .addValue("message", message);

        namedParameterJdbcTemplate.update(sql, parameters);
    }
}
