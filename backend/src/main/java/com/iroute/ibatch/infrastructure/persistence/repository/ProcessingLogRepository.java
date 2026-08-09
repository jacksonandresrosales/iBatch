package com.iroute.ibatch.infrastructure.persistence.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.iroute.ibatch.domain.model.ProcessingLogEntry;

@Repository
public class ProcessingLogRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public ProcessingLogRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, JdbcTemplate jdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
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

    public long countAllLogs() {
        var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processing_logs", Long.class);
        return count == null ? 0 : count;
    }

    public List<ProcessingLogEntry> findRecentPaginated(int limit, int offset) {
        var sql = """
                SELECT pl.log_id, pl.file_id, pl.transaction_id, f.file_name,
                       ll.code AS level, let.code AS event, pl.message, pl.created_at
                FROM processing_logs pl
                LEFT JOIN files f ON f.file_id = pl.file_id
                INNER JOIN log_level ll ON ll.log_level_id = pl.log_level_id
                INNER JOIN log_event_type let ON let.log_event_type_id = pl.log_event_type_id
                ORDER BY pl.created_at DESC, pl.log_id DESC
                LIMIT ? OFFSET ?
                """;
        return jdbcTemplate.query(sql, this::mapLog, limit, offset);
    }

    public List<ProcessingLogEntry> findRecentHighLevel(int limit) {
        var sql = """
                SELECT pl.log_id, pl.file_id, pl.transaction_id, f.file_name,
                       ll.code AS level, let.code AS event, pl.message, pl.created_at
                FROM processing_logs pl
                LEFT JOIN files f ON f.file_id = pl.file_id
                INNER JOIN log_level ll ON ll.log_level_id = pl.log_level_id
                INNER JOIN log_event_type let ON let.log_event_type_id = pl.log_event_type_id
                WHERE pl.log_event_type_id <> 5
                ORDER BY pl.created_at DESC, pl.log_id DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapLog, limit);
    }

    private ProcessingLogEntry mapLog(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        Timestamp timestamp = resultSet.getTimestamp("created_at");
        LocalDateTime createdAt = timestamp == null ? null : timestamp.toLocalDateTime();
        return new ProcessingLogEntry(resultSet.getLong("log_id"), resultSet.getObject("file_id", Long.class),
                resultSet.getObject("transaction_id", Long.class), resultSet.getString("file_name"),
                resultSet.getString("level"), resultSet.getString("event"), resultSet.getString("message"), createdAt);
    }
}
