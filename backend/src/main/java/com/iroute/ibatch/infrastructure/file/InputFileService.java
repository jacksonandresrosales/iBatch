package com.iroute.ibatch.infrastructure.file;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
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
import org.springframework.web.multipart.MultipartFile;

import com.iroute.ibatch.config.FileStorageProperties;
import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.infrastructure.csv.CsvFileValidator;

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
                    .filter(file -> TRANSACTIONS_FILE_PATTERN.matcher(file.getFileName().toString()).matches())
                    .filter(file -> !excludedFileNames.contains(file.getFileName().toString()))
                    .map(this::toResponse)
                    .sorted(Comparator.comparing(AvailableFileResponse::fileName))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el directorio de entrada", exception);
        }
    }

    public AvailableFileResponse storeUploadedCsv(
            MultipartFile uploadedFile,
            Collection<String> unavailableFileNames) {
        var fileName = validateUpload(uploadedFile, unavailableFileNames);
        var inputDir = resolveInputDir();
        Path temporaryFile = null;

        try {
            Files.createDirectories(inputDir);

            var targetFile = inputDir.resolve(fileName).normalize();
            if (!targetFile.startsWith(inputDir)) {
                throw new IllegalArgumentException("El nombre del archivo no es valido");
            }
            if (fileNameExists(inputDir, fileName)) {
                throw new IllegalArgumentException("Ya existe un archivo con el mismo nombre");
            }

            temporaryFile = Files.createTempFile(inputDir, ".ibatch-upload-", ".tmp");
            uploadedFile.transferTo(temporaryFile);

            var storedSize = Files.size(temporaryFile);
            if (storedSize == 0) {
                throw new IllegalArgumentException("El archivo CSV esta vacio");
            }
            if (storedSize > fileStorageProperties.maxSizeBytes()) {
                throw new IllegalArgumentException("El archivo excede el tamano maximo permitido de 50 MB");
            }

            CsvFileValidator.validateUploadedFile(temporaryFile);
            moveWithoutOverwrite(temporaryFile, targetFile);
            temporaryFile = null;

            return toResponse(targetFile);
        } catch (FileAlreadyExistsException exception) {
            throw new IllegalArgumentException("Ya existe un archivo con el mismo nombre", exception);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo almacenar el archivo CSV", exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Best effort cleanup for an incomplete upload.
                }
            }
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

    private String validateUpload(MultipartFile uploadedFile, Collection<String> unavailableFileNames) {
        if (uploadedFile == null || uploadedFile.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo CSV con contenido");
        }

        var fileName = uploadedFile.getOriginalFilename();
        if (fileName == null || fileName.isBlank()
                || !TRANSACTIONS_FILE_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("El archivo debe cumplir el formato transactions_DDMMYYYY.csv");
        }

        extractFileDate(fileName);

        if (uploadedFile.getSize() > fileStorageProperties.maxSizeBytes()) {
            throw new IllegalArgumentException("El archivo excede el tamano maximo permitido de 50 MB");
        }
        if (unavailableFileNames.stream().anyMatch(fileName::equalsIgnoreCase)) {
            throw new IllegalArgumentException("El archivo ya fue registrado para procesamiento");
        }

        return fileName;
    }

    private boolean fileNameExists(Path inputDir, String fileName) throws IOException {
        try (var files = Files.list(inputDir)) {
            return files.anyMatch(path -> path.getFileName().toString().equalsIgnoreCase(fileName));
        }
    }

    private void moveWithoutOverwrite(Path source, Path target) throws IOException {
        Files.move(source, target);
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
