package com.iroute.ibatch.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.iroute.ibatch.config.FileStorageProperties;

class InputFileServiceTest {

    @TempDir
    private Path inputDir;

    @Test
    void shouldListOnlyFilesWithExpectedFormat() throws Exception {
        Files.writeString(inputDir.resolve("transactions_30072026.csv"), "cuenta,monto,fecha");
        Files.writeString(inputDir.resolve("transactions_wrong.csv"), "cuenta,monto,fecha");
        Files.writeString(inputDir.resolve("notes.txt"), "ignore");

        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        var files = service.findAvailableCsvFiles();

        assertThat(files).hasSize(1);
        assertThat(files)
                .extracting("fileName")
                .containsExactly("transactions_30072026.csv");
        assertThat(files.get(0).expectedFormat()).isTrue();
    }

    @Test
    void shouldReturnEmptyListWhenInputDirectoryDoesNotExist() {
        var service = new InputFileService(new FileStorageProperties(inputDir.resolve("missing"), 52_428_800L));

        var files = service.findAvailableCsvFiles();

        assertThat(files).isEmpty();
    }

    @Test
    void shouldValidateExistingFileForProcessing() throws Exception {
        Files.writeString(inputDir.resolve("transactions_30072026.csv"), "cuenta,monto,fecha");
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        var response = service.validateFileForProcessing("transactions_30072026.csv");

        assertThat(response.fileName()).isEqualTo("transactions_30072026.csv");
        assertThat(response.originalPath()).endsWith("transactions_30072026.csv");
        assertThat(response.fileDate()).isEqualTo(LocalDate.parse("2026-07-30"));
    }

    @Test
    void shouldRejectFileWithUnexpectedFormat() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        assertThatThrownBy(() -> service.validateFileForProcessing("transactions 30072026.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo debe cumplir el formato transactions_DDMMYYYY.csv");
    }

    @Test
    void shouldRejectMissingFile() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        assertThatThrownBy(() -> service.validateFileForProcessing("transactions_30072026.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no existe en el directorio configurado");
    }

    @Test
    void shouldStoreValidUploadedCsv() throws Exception {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = csvUpload(
                "transactions_31072026.csv",
                "cuenta,monto,fecha\n2000000000,3241.71,31/07/2026\n");

        var response = service.storeUploadedCsv(upload, List.of());

        assertThat(response.fileName()).isEqualTo("transactions_31072026.csv");
        assertThat(response.sizeBytes()).isEqualTo(upload.getSize());
        assertThat(Files.readString(inputDir.resolve("transactions_31072026.csv")))
                .isEqualTo("cuenta,monto,fecha\n2000000000,3241.71,31/07/2026\n");
    }

    @Test
    void shouldRejectUploadedCsvWithInvalidHeadersAndRemoveTemporaryFile() throws Exception {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = csvUpload(
                "transactions_31072026.csv",
                "cuenta,monto,fecha,origen\n2000000000,3241.71,31/07/2026,web\n");

        assertThatThrownBy(() -> service.storeUploadedCsv(upload, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La estructura del archivo no es valida. Use los encabezados cuenta,monto,fecha");

        assertThat(Files.list(inputDir).toList()).isEmpty();
    }

    @Test
    void shouldRejectUploadedCsvWithInvalidDateInFileName() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = csvUpload(
                "transactions_32072026.csv",
                "cuenta,monto,fecha\n2000000000,3241.71,31/07/2026\n");

        assertThatThrownBy(() -> service.storeUploadedCsv(upload, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha del archivo no es valida");
    }

    @Test
    void shouldRejectUploadedCsvAlreadyRegistered() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = csvUpload(
                "transactions_31072026.csv",
                "cuenta,monto,fecha\n2000000000,3241.71,31/07/2026\n");

        assertThatThrownBy(() -> service.storeUploadedCsv(upload, List.of("TRANSACTIONS_31072026.CSV")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo ya fue registrado para procesamiento");
    }

    @Test
    void shouldRejectEmptyUploadedCsv() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = csvUpload("transactions_31072026.csv", "");

        assertThatThrownBy(() -> service.storeUploadedCsv(upload, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Debe seleccionar un archivo CSV con contenido");
    }

    private MockMultipartFile csvUpload(String fileName, String content) {
        return new MockMultipartFile("file", fileName, "text/csv", content.getBytes());
    }
}
