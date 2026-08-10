package com.iroute.ibatch.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.infrastructure.file.InputFileService;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@Service
public class AvailableFileService {

    private final InputFileService inputFileService;
    private final ProcessedFileRepository processedFileRepository;

    public AvailableFileService(
            InputFileService inputFileService,
            ProcessedFileRepository processedFileRepository) {
        this.inputFileService = inputFileService;
        this.processedFileRepository = processedFileRepository;
    }

    public List<AvailableFileResponse> findAvailableFiles() {
        var registeredFileNames = processedFileRepository.findRegisteredFileNames();

        return inputFileService.findAvailableCsvFiles(registeredFileNames);
    }

    public AvailableFileResponse uploadCsv(MultipartFile file) {
        var registeredFileNames = processedFileRepository.findRegisteredFileNames();
        return inputFileService.storeUploadedCsv(file, registeredFileNames);
    }
}
