package com.iroute.ibatch.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.application.usecase.TransactionBatchWriter;
import com.iroute.ibatch.config.ProcessingProperties;
import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.domain.model.TransactionBatchResult;
import com.iroute.ibatch.domain.rule.TransactionValidationRule;

@ExtendWith(MockitoExtension.class)
class CsvTransactionProcessorTest {

    @TempDir
    private Path inputDir;

    @Mock
    private TransactionBatchWriter transactionBatchWriter;

    @Test
    void shouldProcessRowsInBatch() throws Exception {
        var csv = inputDir.resolve("transactions_31072026.csv");
        Files.writeString(csv, """
                cuenta,monto,fecha
                2000000000,3241.71,31/07/2026
                abc,xyz,31/07/2026
                """);
        var inputFile = new InputFileMetadata(
                "transactions_31072026.csv",
                csv.toString(),
                LocalDate.parse("2026-07-31"));
        when(transactionBatchWriter.write(eq(10L), anyList()))
                .thenReturn(new TransactionBatchResult(1, 1));
        var processor = new CsvTransactionProcessor(
                new CsvReader(),
                new TransactionValidationRule(List.of(
                        new com.iroute.ibatch.domain.rule.validator.AccountValidator(),
                        new com.iroute.ibatch.domain.rule.validator.AmountValidator(),
                        new com.iroute.ibatch.domain.rule.validator.DateValidator(),
                        new com.iroute.ibatch.domain.rule.validator.DuplicateValidator()
                )),
                transactionBatchWriter,
                new ProcessingProperties(500, 1_000_000),
                new com.iroute.ibatch.application.usecase.FileProgressTracker());

        var result = processor.process(10L, inputFile);

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.rejectedCount()).isEqualTo(1);
        verify(transactionBatchWriter).write(eq(10L), anyList());
    }

    @Test
    void shouldRejectUnexpectedHeaders() throws Exception {
        var csv = inputDir.resolve("transactions_31072026.csv");
        Files.writeString(csv, "cuenta,monto,fecha,origen\n2000000000,3241.71,31/07/2026,web");
        var inputFile = new InputFileMetadata(
                "transactions_31072026.csv",
                csv.toString(),
                LocalDate.parse("2026-07-31"));
        var processor = new CsvTransactionProcessor(
                new CsvReader(),
                new TransactionValidationRule(List.of(
                        new com.iroute.ibatch.domain.rule.validator.AccountValidator(),
                        new com.iroute.ibatch.domain.rule.validator.AmountValidator(),
                        new com.iroute.ibatch.domain.rule.validator.DateValidator(),
                        new com.iroute.ibatch.domain.rule.validator.DuplicateValidator()
                )),
                transactionBatchWriter,
                new ProcessingProperties(500, 1_000_000),
                new com.iroute.ibatch.application.usecase.FileProgressTracker());

        assertThatThrownBy(() -> processor.process(10L, inputFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La estructura del archivo no es valida");
    }
}
