package com.iroute.ibatch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.domain.model.ProcessedFile;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@ExtendWith(MockitoExtension.class)
class ProcessedFileServiceTest {

    @Mock
    private ProcessedFileRepository processedFileRepository;

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

        var service = new ProcessedFileService(processedFileRepository);
        var response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().fileName()).isEqualTo("transactions_30072026.csv");
        assertThat(response.getFirst().status()).isEqualTo("PROCESADO");
        assertThat(response.getFirst().rejectedTransactions()).isEqualTo(2);
    }
}
