package com.iroute.ibatch.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iroute.ibatch.config.FileStorageProperties;

import java.nio.file.Path;

class InputFileServiceTest {

    @TempDir
    private Path inputDir;

    @Test
    void shouldListOnlyCsvFilesAndDetectExpectedFormat() throws Exception {
        Files.writeString(inputDir.resolve("transactions_30072026.csv"), "cuenta,monto,fecha");
        Files.writeString(inputDir.resolve("transactions_wrong.csv"), "cuenta,monto,fecha");
        Files.writeString(inputDir.resolve("notes.txt"), "ignore");

        var service = new InputFileService(new FileStorageProperties(inputDir));

        var files = service.findAvailableCsvFiles();

        assertThat(files).hasSize(2);
        assertThat(files)
                .extracting("fileName")
                .containsExactly("transactions_30072026.csv", "transactions_wrong.csv");
        assertThat(files.get(0).expectedFormat()).isTrue();
        assertThat(files.get(1).expectedFormat()).isFalse();
    }

    @Test
    void shouldReturnEmptyListWhenInputDirectoryDoesNotExist() {
        var service = new InputFileService(new FileStorageProperties(inputDir.resolve("missing")));

        var files = service.findAvailableCsvFiles();

        assertThat(files).isEmpty();
    }
}
