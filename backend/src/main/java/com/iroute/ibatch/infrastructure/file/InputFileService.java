package com.iroute.ibatch.infrastructure.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.iroute.ibatch.config.FileStorageProperties;
import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.dto.response.AvailableFileResponse;

@Service
public class InputFileService {

    private static final Pattern TRANSACTIONS_FILE_PATTERN =
            Pattern.compile("^transactions_\\d{8}\\.csv$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final FileStorageProperties fileStorageProperties;

    public InputFileService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    public List<AvailableFileResponse> findAvailableCsvFiles() {
        return findAvailableCsvFiles(List.of());
    }

    public List<AvailableFileResponse> findAvailableCsvFiles(Collection<String> excludedFileNames) {
        var inputDir = resolveInputDir();

        if (!Files.isDirectory(inputDir)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(inputDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isCsvFile)
                    .filter(file -> !excludedFileNames.contains(file.getFileName().toString()))
                    .map(this::toResponse)
                    .sorted(Comparator.comparing(AvailableFileResponse::fileName))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el directorio de entrada", exception);
        }
    }

    public InputFileMetadata validateFileForProcessing(String fileName) {
        if (!TRANSACTIONS_FILE_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("El archivo debe cumplir el formato transactions_DDMMYYYY.csv");
        }

        var inputDir = resolveInputDir();
        var filePath = inputDir.resolve(fileName).normalize();

        if (!filePath.startsWith(inputDir)) {
            throw new IllegalArgumentException("El nombre del archivo no es valido");
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("El archivo no existe en el directorio configurado");
        }

        try {
            if (Files.size(filePath) > fileStorageProperties.maxSizeBytes()) {
                throw new IllegalArgumentException("El archivo excede el tamano maximo permitido");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo validar el tamano del archivo", exception);
        }

        return new InputFileMetadata(
                fileName,
                filePath.toString(),
                extractFileDate(fileName));
    }

    private Path resolveInputDir() {
        return fileStorageProperties.inputDir().toAbsolutePath().normalize();
    }

    private boolean isCsvFile(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private LocalDate extractFileDate(String fileName) {
        try {
            var datePart = fileName.substring("transactions_".length(), "transactions_".length() + 8);

            return LocalDate.parse(datePart, FILE_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("La fecha del archivo no es valida");
        }
    }

    private AvailableFileResponse toResponse(Path file) {
        try {
            var fileName = file.getFileName().toString();
            var lastModifiedAt = OffsetDateTime.ofInstant(
                    Files.getLastModifiedTime(file).toInstant(),
                    ZoneId.systemDefault());

            return new AvailableFileResponse(
                    fileName,
                    Files.size(file),
                    lastModifiedAt,
                    TRANSACTIONS_FILE_PATTERN.matcher(fileName).matches());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer la informacion del archivo " + file, exception);
        }
    }
}
