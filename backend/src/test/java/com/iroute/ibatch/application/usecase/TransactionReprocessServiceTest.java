package com.iroute.ibatch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.domain.model.FileTransactionCounters;
import com.iroute.ibatch.domain.model.ProcessedTransaction;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionReprocessServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProcessedFileRepository processedFileRepository;

    @Mock
    private ProcessingLogRepository processingLogRepository;

    @Test
    void shouldReprocessRejectedTransactionAsProcessed() {
        var transaction = rejectedTransaction("RECHAZADA");

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.findRejectionsByTransactionId(10L)).thenReturn(List.of());
        when(transactionRepository.existsProcessedUniqueKeyExcludingTransaction("2000000000|2026-07-31|125.50", 10L))
                .thenReturn(false);
        when(transactionRepository.countByFileId(1L)).thenReturn(new FileTransactionCounters(3, 3, 0));

        var service = new TransactionReprocessService(
                transactionRepository,
                processedFileRepository,
                processingLogRepository);
        var response = service.reprocessAmount(10L, new BigDecimal("125.50"));

        assertThat(response.status()).isEqualTo("PROCESADO");
        verify(transactionRepository).updateReprocessedAmount(10L, new BigDecimal("125.50"), 1, "2000000000|2026-07-31|125.50");
        verify(transactionRepository).deleteRejections(10L);
        verify(transactionRepository).saveReprocessHistory(transaction, new BigDecimal("125.50"), 1, null, null);
        verify(processedFileRepository).updateCounters(1L, 3, 3, 3, 0);
    }

    @Test
    void shouldRejectProcessedTransaction() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(rejectedTransaction("PROCESADO")));

        var service = new TransactionReprocessService(
                transactionRepository,
                processedFileRepository,
                processingLogRepository);

        assertThatThrownBy(() -> service.reprocessAmount(10L, new BigDecimal("125.50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solo se pueden reprocesar transacciones rechazadas");
    }

    @Test
    void shouldRejectMissingTransaction() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.empty());

        var service = new TransactionReprocessService(
                transactionRepository,
                processedFileRepository,
                processingLogRepository);

        assertThatThrownBy(() -> service.reprocessAmount(10L, new BigDecimal("125.50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La transaccion no existe");
    }

    private ProcessedTransaction rejectedTransaction(String status) {
        return new ProcessedTransaction(
                10L,
                1L,
                2,
                "2000000000",
                "abc",
                "31/07/2026",
                "2000000000",
                null,
                LocalDate.parse("2026-07-31"),
                status,
                LocalDateTime.parse("2026-07-30T18:51:00"),
                LocalDateTime.parse("2026-07-30T18:52:00"));
    }
}
