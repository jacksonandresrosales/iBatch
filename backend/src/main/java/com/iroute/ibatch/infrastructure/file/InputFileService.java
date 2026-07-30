package com.iroute.ibatch.infrastructure.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.iroute.ibatch.config.FileStorageProperties;
import com.iroute.ibatch.dto.response.AvailableFileResponse;

@Service
public class InputFileService {

    private static final Pattern TRANSACTIONS_FILE_PATTERN =
            Pattern.compile("^transactions_\\d{8}\\.csv$", Pattern.CASE_INSENSITIVE);

    private final FileStorageProperties fileStorageProperties;

    public InputFileService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    public List<AvailableFileResponse> findAvailableCsvFiles() {
        var inputDir = resolveInputDir();

        if (!Files.isDirectory(inputDir)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(inputDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isCsvFile)
                    .map(this::toResponse)
                    .sorted(Comparator.comparing(AvailableFileResponse::fileName))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el directorio de entrada", exception);
        }
    }

    private Path resolveInputDir() {
        return fileStorageProperties.inputDir().toAbsolutePath().normalize();
    }

    private boolean isCsvFile(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv");
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
