package com.iroute.ibatch.infrastructure.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Service;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.domain.model.TransactionProcessingResult;
import com.iroute.ibatch.domain.model.TransactionRejection;
import com.iroute.ibatch.domain.rule.TransactionValidationRule;
import com.iroute.ibatch.application.usecase.FileProgressTracker;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@Service
public class CsvTransactionProcessor {

    private static final int STATUS_RECHAZADA = 2;
    private static final int FILA_CORRUPTA = 8;
    private static final Set<String> REQUIRED_HEADERS = Set.of("cuenta", "monto", "fecha");

    private final TransactionValidationRule transactionValidationRule;
    private final TransactionRepository transactionRepository;

    public CsvTransactionProcessor(
            TransactionValidationRule transactionValidationRule,
            TransactionRepository transactionRepository) {
        this.transactionValidationRule = transactionValidationRule;
        this.transactionRepository = transactionRepository;
    }

    public TransactionProcessingResult process(Long fileId, InputFileMetadata inputFile) {
        return process(fileId, inputFile, null);
    }

    public TransactionProcessingResult process(Long fileId, InputFileMetadata inputFile, FileProgressTracker progressTracker) {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(inputFile.originalPath()))) {
            var csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();
            var parser = csvFormat.parse(reader);

            validateHeaders(parser.getHeaderMap().keySet());

            var currentFileUniqueKeys = new HashSet<String>();
            var processedCount = 0;
            var rejectedCount = 0;
            var totalRecords = countRecords(inputFile);
            if (progressTracker != null) {
                progressTracker.startProgress(fileId, inputFile.fileName(), totalRecords);
            }

            for (var record : parser) {
                if (!record.isConsistent()) {
                    saveCorruptedRow(fileId, Math.toIntExact(record.getRecordNumber() + 1));
                    rejectedCount++;
                    updateProgress(progressTracker, fileId, inputFile.fileName(), processedCount, rejectedCount, totalRecords);
                    continue;
                }

                var validatedTransaction = transactionValidationRule.validate(record, currentFileUniqueKeys);
                var transactionId = transactionRepository.save(fileId, validatedTransaction.row());

                if (validatedTransaction.rejections().isEmpty()) {
                    processedCount++;
                } else {
                    transactionRepository.saveRejections(transactionId, validatedTransaction.rejections());
                    rejectedCount++;
                }
                updateProgress(progressTracker, fileId, inputFile.fileName(), processedCount, rejectedCount, totalRecords);
            }

            return new TransactionProcessingResult(processedCount + rejectedCount, processedCount, rejectedCount);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo CSV", exception);
        }
    }

    private int countRecords(InputFileMetadata inputFile) {
        try (var lines = Files.lines(Path.of(inputFile.originalPath()))) {
            return (int) Math.max(0, lines.count() - 1);
        } catch (IOException exception) {
            return 0;
        }
    }

    private void updateProgress(FileProgressTracker tracker, Long fileId, String fileName,
            int processedCount, int rejectedCount, int totalRecords) {
        if (tracker != null) {
            tracker.updateProgress(fileId, fileName, processedCount, rejectedCount, totalRecords);
        }
    }

    private void validateHeaders(Set<String> headers) {
        if (!headers.containsAll(REQUIRED_HEADERS)) {
            throw new IllegalArgumentException("La estructura del archivo no es valida");
        }
    }

    private void saveCorruptedRow(Long fileId, int lineNumber) {
        var row = new CsvTransactionRow(
                lineNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                STATUS_RECHAZADA,
                null);
        var transactionId = transactionRepository.save(fileId, row);
        var rejection = new TransactionRejection(FILA_CORRUPTA, "La fila no contiene la estructura esperada");

        transactionRepository.saveRejections(transactionId, List.of(rejection));
    }
}
