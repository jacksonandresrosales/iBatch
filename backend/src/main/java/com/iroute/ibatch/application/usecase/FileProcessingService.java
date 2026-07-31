package com.iroute.ibatch.application.usecase;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.iroute.ibatch.dto.response.ProcessFileResponse;
import com.iroute.ibatch.infrastructure.file.InputFileService;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@Service
public class FileProcessingService {

    private final InputFileService inputFileService;
    private final ProcessedFileRepository processedFileRepository;

    public FileProcessingService(
            InputFileService inputFileService,
            ProcessedFileRepository processedFileRepository) {
        this.inputFileService = inputFileService;
        this.processedFileRepository = processedFileRepository;
    }

    public ProcessFileResponse registerFileForProcessing(String fileName) {
        if (processedFileRepository.existsByFileName(fileName)) {
            throw new IllegalArgumentException("El archivo ya fue registrado para procesamiento");
        }

        var inputFile = inputFileService.validateFileForProcessing(fileName);

        try {
            var fileId = processedFileRepository.saveProcessing(inputFile);

            return new ProcessFileResponse(
                    fileId,
                    inputFile.fileName(),
                    "PROCESANDO",
                    "Archivo registrado para procesamiento");
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("El archivo ya fue registrado para procesamiento");
        }
    }
}
