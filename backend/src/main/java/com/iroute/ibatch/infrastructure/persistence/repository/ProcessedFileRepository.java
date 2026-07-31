package com.iroute.ibatch.infrastructure.persistence.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.iroute.ibatch.domain.model.ProcessedFile;

@Repository
public class ProcessedFileRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProcessedFile> findAll() {
        var sql = """
                SELECT f.file_id AS id,
                       f.file_name,
                       fs.code AS status,
                       f.total_records AS total_transactions,
                       f.processed_count AS processed_transactions,
                       f.rejected_count AS rejected_transactions,
                       NULL AS error_message,
                       f.created_at,
                       f.updated_at
                FROM files f
                INNER JOIN file_status fs
                        ON fs.file_status_id = f.file_status_id
                ORDER BY f.created_at DESC, f.file_id DESC
                """;

        return jdbcTemplate.query(sql, this::mapRow);
    }

    private ProcessedFile mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProcessedFile(
                resultSet.getLong("id"),
                resultSet.getString("file_name"),
                resultSet.getString("status"),
                resultSet.getInt("total_transactions"),
                resultSet.getInt("processed_transactions"),
                resultSet.getInt("rejected_transactions"),
                resultSet.getString("error_message"),
                toLocalDateTime(resultSet.getTimestamp("created_at")),
                toLocalDateTime(resultSet.getTimestamp("updated_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
