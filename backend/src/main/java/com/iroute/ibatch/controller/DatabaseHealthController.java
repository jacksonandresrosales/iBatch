package com.iroute.ibatch.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.dto.response.DatabaseHealthResponse;

@RestController
@RequestMapping("/api/health/database")
public class DatabaseHealthController {

    private final DataSource dataSource;

    public DatabaseHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<DatabaseHealthResponse> health() {
        try (Connection connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
                var response = new DatabaseHealthResponse(
                    true,
                    "Conexion con la base de datos disponible",
                    metadata.getDatabaseProductName(),
                    metadata.getURL(),
                    OffsetDateTime.now());

            return ResponseEntity.ok(response);
        } catch (SQLException exception) {
                var response = new DatabaseHealthResponse(
                    false,
                    "No se pudo conectar con la base de datos: " + exception.getMessage(),
                    null,
                    null,
                    OffsetDateTime.now());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
