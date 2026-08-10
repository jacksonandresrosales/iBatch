package com.iroute.ibatch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.infrastructure.file.InputFileService;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@ExtendWith(MockitoExtension.class)
class AvailableFileServiceTest {

    @Mock
    private InputFileService inputFileService;

    @Mock
    private ProcessedFileRepository processedFileRepository;

    @Test
    void shouldExcludeRegisteredFilesWhenListingAvailableFiles() {
        var registeredFileNames = Set.of("transactions_30072026.csv");
        var availableFiles = List.of(new AvailableFileResponse(
                "transactions_31072026.csv",
                1024,
                OffsetDateTime.parse("2026-07-30T18:30:00-05:00"),
                true));

        when(processedFileRepository.findRegisteredFileNames()).thenReturn(registeredFileNames);
        when(inputFileService.findAvailableCsvFiles(registeredFileNames)).thenReturn(availableFiles);

        var service = new AvailableFileService(inputFileService, processedFileRepository);
        var response = service.findAvailableFiles();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().fileName()).isEqualTo("transactions_31072026.csv");
    }

    @Test
    void shouldStoreUploadExcludingRegisteredFileNames() {
        var registeredFileNames = Set.of("transactions_30072026.csv");
        var upload = new MockMultipartFile(
                "file",
                "transactions_31072026.csv",
                "text/csv",
                "cuenta,monto,fecha\n2000000000,10.00,31/07/2026\n".getBytes());
        var storedFile = new AvailableFileResponse(
                "transactions_31072026.csv",
                upload.getSize(),
                OffsetDateTime.parse("2026-07-30T18:30:00-05:00"),
                true);

        when(processedFileRepository.findRegisteredFileNames()).thenReturn(registeredFileNames);
        when(inputFileService.storeUploadedCsv(upload, registeredFileNames)).thenReturn(storedFile);

        var service = new AvailableFileService(inputFileService, processedFileRepository);
        var response = service.uploadCsv(upload);

        assertThat(response).isEqualTo(storedFile);
        verify(inputFileService).storeUploadedCsv(upload, registeredFileNames);
    }
}
