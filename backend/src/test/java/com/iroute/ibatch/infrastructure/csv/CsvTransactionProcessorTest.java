package com.iroute.ibatch.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.domain.rule.TransactionValidationRule;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class CsvTransactionProcessorTest {

    @TempDir
    private Path inputDir;

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void shouldProcessValidAndRejectedRows() throws Exception {
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

        when(transactionRepository.existsProcessedUniqueKey("2000000000|2026-07-31|3241.71")).thenReturn(false);
        when(transactionRepository.save(eq(10L), any(CsvTransactionRow.class))).thenReturn(1L, 2L);

        var validationRule = new TransactionValidationRule(transactionRepository);
        var processor = new CsvTransactionProcessor(validationRule, transactionRepository);
        var result = processor.process(10L, inputFile);

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.rejectedCount()).isEqualTo(1);
        verify(transactionRepository).saveRejections(eq(2L), any());
    }
}
