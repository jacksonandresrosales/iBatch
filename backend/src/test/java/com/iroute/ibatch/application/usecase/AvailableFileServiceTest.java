package com.iroute.ibatch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
