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
import com.iroute.ibatch.domain.model.TransactionProcessingResult;
import com.iroute.ibatch.infrastructure.csv.CsvTransactionProcessor;
import com.iroute.ibatch.infrastructure.file.InputFileService;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock
    private InputFileService inputFileService;

    @Mock
    private ProcessedFileRepository processedFileRepository;

    @Mock
    private CsvTransactionProcessor csvTransactionProcessor;

    @Mock
    private ProcessingLogRepository processingLogRepository;

    @Test
    void shouldRegisterFileForProcessing() {
        var fileName = "transactions_30072026.csv";
        var inputFile = new InputFileMetadata(fileName, "C:/input/transactions_30072026.csv", LocalDate.parse("2026-07-30"));
        var result = new TransactionProcessingResult(2, 2, 0);

        when(processedFileRepository.existsByFileName(fileName)).thenReturn(false);
        when(inputFileService.validateFileForProcessing(fileName)).thenReturn(inputFile);
        when(processedFileRepository.saveProcessing(inputFile)).thenReturn(7L);
        when(csvTransactionProcessor.process(7L, inputFile)).thenReturn(result);

        var service = new FileProcessingService(
                inputFileService,
                processedFileRepository,
                csvTransactionProcessor,
                processingLogRepository);
        var response = service.registerFileForProcessing(fileName);

        assertThat(response.fileId()).isEqualTo(7L);
        assertThat(response.fileName()).isEqualTo(fileName);
        assertThat(response.status()).isEqualTo("PROCESADO");
        assertThat(response.totalRecords()).isEqualTo(2);
        assertThat(response.processedCount()).isEqualTo(2);
        assertThat(response.rejectedCount()).isZero();
        verify(processedFileRepository).saveProcessing(inputFile);
        verify(processedFileRepository).updateFinished(7L, 3, 2, 2, 0);
    }

    @Test
    void shouldRejectAlreadyRegisteredFile() {
        var fileName = "transactions_30072026.csv";

        when(processedFileRepository.existsByFileName(fileName)).thenReturn(true);

        var service = new FileProcessingService(
                inputFileService,
                processedFileRepository,
                csvTransactionProcessor,
                processingLogRepository);

        assertThatThrownBy(() -> service.registerFileForProcessing(fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo ya fue registrado para procesamiento");
    }
}
