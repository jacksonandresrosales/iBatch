package com.iroute.ibatch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.domain.model.ProcessedFile;
import com.iroute.ibatch.domain.model.ProcessedTransaction;
import com.iroute.ibatch.domain.model.TransactionRejectionDetail;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class ProcessedFileServiceTest {

    @Mock
    private ProcessedFileRepository processedFileRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void shouldReturnProcessedFilesFromRepository() {
        var processedFile = new ProcessedFile(
                1L,
                "transactions_30072026.csv",
                "PROCESADO",
                10,
                8,
                2,
                null,
                LocalDateTime.parse("2026-07-30T18:50:00"),
                LocalDateTime.parse("2026-07-30T18:55:00"));

        when(processedFileRepository.findAll()).thenReturn(List.of(processedFile));

        var service = new ProcessedFileService(processedFileRepository, transactionRepository);
        var response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().fileName()).isEqualTo("transactions_30072026.csv");
        assertThat(response.getFirst().status()).isEqualTo("PROCESADO");
        assertThat(response.getFirst().rejectedTransactions()).isEqualTo(2);
    }

    @Test
    void shouldReturnProcessedFileDetail() {
        var processedFile = new ProcessedFile(
                1L,
                "transactions_30072026.csv",
                "PROCESADO_CON_RECHAZOS",
                2,
                1,
                1,
                null,
                LocalDateTime.parse("2026-07-30T18:50:00"),
                LocalDateTime.parse("2026-07-30T18:55:00"));
        var transaction = new ProcessedTransaction(
                10L,
                1L,
                2,
                "2000000000",
                "xyz",
                "31/07/2026",
                "2000000000",
                null,
                LocalDate.parse("2026-07-31"),
                "RECHAZADA",
                LocalDateTime.parse("2026-07-30T18:51:00"),
                LocalDateTime.parse("2026-07-30T18:52:00"));
        var rejection = new TransactionRejectionDetail(
                100L,
                10L,
                "MONTO_INVALIDO",
                "Monto invalido",
                "El monto debe ser un valor monetario valido",
                LocalDateTime.parse("2026-07-30T18:52:00"));

        when(processedFileRepository.findById(1L)).thenReturn(Optional.of(processedFile));
        when(transactionRepository.findByFileId(1L, 0, 50, null, "2000000000"))
                .thenReturn(List.of(transaction));
        when(transactionRepository.findRejectionsByTransactionIds(List.of(10L))).thenReturn(List.of(rejection));
        when(transactionRepository.countByFileId(1L, null, "2000000000")).thenReturn(1L);

        var service = new ProcessedFileService(processedFileRepository, transactionRepository);
        var response = service.findDetailById(1L, 0, 50, null, " 2000000000 ");

        assertThat(response.file().id()).isEqualTo(1L);
        assertThat(response.transactions()).hasSize(1);
        assertThat(response.transactions().getFirst().status()).isEqualTo("RECHAZADA");
        assertThat(response.transactions().getFirst().rejections()).hasSize(1);
        assertThat(response.transactions().getFirst().rejections().getFirst().reasonCode()).isEqualTo("MONTO_INVALIDO");
    }
}
