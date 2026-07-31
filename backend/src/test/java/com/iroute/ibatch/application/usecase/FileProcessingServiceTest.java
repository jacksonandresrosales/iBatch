package com.iroute.ibatch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.infrastructure.file.InputFileService;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock
    private InputFileService inputFileService;

    @Mock
    private ProcessedFileRepository processedFileRepository;

    @Test
    void shouldRegisterFileForProcessing() {
        var fileName = "transactions_30072026.csv";
        var inputFile = new InputFileMetadata(fileName, "C:/input/transactions_30072026.csv", LocalDate.parse("2026-07-30"));

        when(processedFileRepository.existsByFileName(fileName)).thenReturn(false);
        when(inputFileService.validateFileForProcessing(fileName)).thenReturn(inputFile);
        when(processedFileRepository.saveProcessing(inputFile)).thenReturn(7L);

        var service = new FileProcessingService(inputFileService, processedFileRepository);
        var response = service.registerFileForProcessing(fileName);

        assertThat(response.fileId()).isEqualTo(7L);
        assertThat(response.fileName()).isEqualTo(fileName);
        assertThat(response.status()).isEqualTo("PROCESANDO");
        verify(processedFileRepository).saveProcessing(inputFile);
    }

    @Test
    void shouldRejectAlreadyRegisteredFile() {
        var fileName = "transactions_30072026.csv";

        when(processedFileRepository.existsByFileName(fileName)).thenReturn(true);

        var service = new FileProcessingService(inputFileService, processedFileRepository);

        assertThatThrownBy(() -> service.registerFileForProcessing(fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo ya fue registrado para procesamiento");
    }
}
