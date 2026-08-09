package com.iroute.ibatch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.TransactionRejection;
import com.iroute.ibatch.domain.model.ValidatedCsvTransaction;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionBatchWriterTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProcessingLogRepository processingLogRepository;

    @Test
    void shouldPersistRejectedRowWithoutProcessedUniqueKey() {
        var row = new CsvTransactionRow(2, "", "100", "2026-07-31", null, null, null, 2, null);
        var transaction = new ValidatedCsvTransaction(
                row,
                List.of(new TransactionRejection(1, "La cuenta es obligatoria")));

        when(transactionRepository.findExistingProcessedUniqueKeys(Set.of())).thenReturn(Set.of());
        when(transactionRepository.findIdsByFileAndLineNumbers(1L, List.of(2))).thenReturn(Map.of(2, 10L));

        var writer = new TransactionBatchWriter(transactionRepository, processingLogRepository);
        var result = writer.write(1L, List.of(transaction));

        assertThat(result.processedCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(1);
    }
}
