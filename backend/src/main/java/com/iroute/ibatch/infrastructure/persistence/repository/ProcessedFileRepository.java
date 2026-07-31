package com.iroute.ibatch.infrastructure.persistence.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.domain.model.ProcessedFile;

@Repository
public class ProcessedFileRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProcessedFileRepository(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
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

    public Set<String> findRegisteredFileNames() {
        var sql = """
                SELECT file_name
                FROM files
                WHERE record_status_id = 1
                """;

        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class));
    }

    public boolean existsByFileName(String fileName) {
        var sql = """
                SELECT COUNT(1)
                FROM files
                WHERE file_name = ?
                """;

        var count = jdbcTemplate.queryForObject(sql, Integer.class, fileName);

        return count != null && count > 0;
    }

    public Long saveProcessing(InputFileMetadata inputFile) {
        var sql = """
                INSERT INTO files (
                    file_name,
                    original_path,
                    file_date,
                    file_status_id,
                    started_at,
                    record_status_id
                ) VALUES (
                    :fileName,
                    :originalPath,
                    :fileDate,
                    2,
                    CURRENT_TIMESTAMP,
                    1
                )
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("fileName", inputFile.fileName())
                .addValue("originalPath", inputFile.originalPath())
                .addValue("fileDate", inputFile.fileDate());
        var keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, parameters, keyHolder, new String[] {"file_id"});

        return keyHolder.getKeyAs(Long.class);
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
