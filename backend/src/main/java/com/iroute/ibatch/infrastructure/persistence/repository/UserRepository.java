package com.iroute.ibatch.infrastructure.persistence.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.iroute.ibatch.domain.model.AppUser;
import com.iroute.ibatch.domain.model.UserRole;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AppUser> findByUsername(String username) {
        var sql = """
                SELECT user_id, username, password_hash, role, enabled
                FROM users
                WHERE username = ?
                """;

        return jdbcTemplate.query(sql, this::mapRow, username).stream().findFirst();
    }

    public void create(String username, String passwordHash, UserRole role) {
        var sql = """
                INSERT INTO users (username, password_hash, role, enabled)
                VALUES (?, ?, ?, TRUE)
                """;

        jdbcTemplate.update(sql, username, passwordHash, role.name());
    }

    private AppUser mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AppUser(
                resultSet.getLong("user_id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                UserRole.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("enabled"));
    }
}
