package com.iroute.ibatch.infrastructure.csv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Service;

import com.iroute.ibatch.application.usecase.FileProgressTracker;
import com.iroute.ibatch.application.usecase.TransactionBatchWriter;
import com.iroute.ibatch.config.ProcessingProperties;
import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.domain.model.TransactionProcessingResult;
import com.iroute.ibatch.domain.model.TransactionRejection;
import com.iroute.ibatch.domain.model.ValidatedCsvTransaction;
import com.iroute.ibatch.domain.rule.TransactionValidationRule;

@Service
public class CsvTransactionProcessor {

    private static final int STATUS_RECHAZADA = 2;
    private static final int FILA_CORRUPTA = 8;

    private final CsvReader csvReader;
    private final TransactionValidationRule transactionValidationRule;
    private final TransactionBatchWriter transactionBatchWriter;
    private final ProcessingProperties processingProperties;
    private final FileProgressTracker fileProgressTracker;

    public CsvTransactionProcessor(
            CsvReader csvReader,
            TransactionValidationRule transactionValidationRule,
            TransactionBatchWriter transactionBatchWriter,
            ProcessingProperties processingProperties,
            FileProgressTracker fileProgressTracker) {
        this.csvReader = csvReader;
        this.transactionValidationRule = transactionValidationRule;
        this.transactionBatchWriter = transactionBatchWriter;
        this.processingProperties = processingProperties;
        this.fileProgressTracker = fileProgressTracker;
    }

    public TransactionProcessingResult process(Long fileId, InputFileMetadata inputFile) {
        int totalFileRecords = csvReader.countTotalRecords(inputFile);
        try (CSVParser parser = csvReader.openParser(inputFile)) {
            var currentFileUniqueKeys = new HashSet<String>();
            var batch = new ArrayList<ValidatedCsvTransaction>(processingProperties.batchSize());
            var totalRecords = 0;
            var processedCount = 0;
            var rejectedCount = 0;

            for (var record : parser) {
                totalRecords++;
                if (totalRecords > processingProperties.maxRecords()) {
                    throw new IllegalArgumentException("El archivo excede el numero maximo de registros permitido");
                }

                batch.add(record.isConsistent()
                        ? transactionValidationRule.validate(record, currentFileUniqueKeys)
                        : corruptedRow(Math.toIntExact(record.getRecordNumber() + 1)));

                if (batch.size() >= processingProperties.batchSize()) {
                    var result = transactionBatchWriter.write(fileId, batch);
                    processedCount += result.processedCount();
                    rejectedCount += result.rejectedCount();
                    batch.clear();

                    fileProgressTracker.updateProgress(fileId, inputFile.fileName(), processedCount, rejectedCount, totalFileRecords > 0 ? totalFileRecords : totalRecords);
                }
            }

            if (!batch.isEmpty()) {
                var result = transactionBatchWriter.write(fileId, batch);
                processedCount += result.processedCount();
                rejectedCount += result.rejectedCount();

                fileProgressTracker.updateProgress(fileId, inputFile.fileName(), processedCount, rejectedCount, totalFileRecords > 0 ? totalFileRecords : totalRecords);
            }

            return new TransactionProcessingResult(totalRecords, processedCount, rejectedCount);
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) exception;
            }
            throw new IllegalStateException("Ocurrio un error al procesar el archivo CSV", exception);
        }
    }

    private ValidatedCsvTransaction corruptedRow(int lineNumber) {
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

        return new ValidatedCsvTransaction(
                row,
                List.of(new TransactionRejection(FILA_CORRUPTA, "La fila no contiene la estructura esperada")));
    }
}
