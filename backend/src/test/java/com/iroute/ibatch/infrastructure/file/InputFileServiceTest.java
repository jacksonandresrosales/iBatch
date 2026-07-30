package com.iroute.ibatch.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iroute.ibatch.config.FileStorageProperties;

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

    @Test
    void shouldValidateExistingFileForProcessing() throws Exception {
        Files.writeString(inputDir.resolve("transactions_30072026.csv"), "cuenta,monto,fecha");
        var service = new InputFileService(new FileStorageProperties(inputDir));

        var response = service.validateFileForProcessing("transactions_30072026.csv");

        assertThat(response.fileName()).isEqualTo("transactions_30072026.csv");
        assertThat(response.status()).isEqualTo("VALIDATED");
    }

    @Test
    void shouldRejectFileWithUnexpectedFormat() {
        var service = new InputFileService(new FileStorageProperties(inputDir));

        assertThatThrownBy(() -> service.validateFileForProcessing("transactions 30072026.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo debe cumplir el formato transactions_DDMMYYYY.csv");
    }

    @Test
    void shouldRejectMissingFile() {
        var service = new InputFileService(new FileStorageProperties(inputDir));

        assertThatThrownBy(() -> service.validateFileForProcessing("transactions_30072026.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no existe en el directorio configurado");
    }
}
