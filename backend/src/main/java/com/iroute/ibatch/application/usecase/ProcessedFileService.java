package com.iroute.ibatch.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.iroute.ibatch.domain.model.ProcessedFile;
import com.iroute.ibatch.dto.response.ProcessedFileResponse;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@Service
public class ProcessedFileService {

    private final ProcessedFileRepository processedFileRepository;

    public ProcessedFileService(ProcessedFileRepository processedFileRepository) {
        this.processedFileRepository = processedFileRepository;
    }

    public List<ProcessedFileResponse> findAll() {
        return processedFileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ProcessedFileResponse toResponse(ProcessedFile processedFile) {
        return new ProcessedFileResponse(
                processedFile.id(),
                processedFile.fileName(),
                processedFile.status(),
                processedFile.totalTransactions(),
                processedFile.processedTransactions(),
                processedFile.rejectedTransactions(),
                processedFile.errorMessage(),
                processedFile.createdAt(),
                processedFile.updatedAt());
    }
}
