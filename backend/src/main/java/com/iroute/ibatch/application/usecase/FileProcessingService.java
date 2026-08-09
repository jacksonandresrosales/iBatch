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
    private final FileProcessingWorker fileProcessingWorker;

    public FileProcessingService(
            InputFileService inputFileService,
            ProcessedFileRepository processedFileRepository,
            FileProcessingWorker fileProcessingWorker) {
        this.inputFileService = inputFileService;
        this.processedFileRepository = processedFileRepository;
        this.fileProcessingWorker = fileProcessingWorker;
    }

    public ProcessFileResponse registerFileForProcessing(String fileName) {
        if (processedFileRepository.existsByFileName(fileName)) {
            throw new IllegalArgumentException("El archivo ya fue registrado para procesamiento");
        }

        var inputFile = inputFileService.validateFileForProcessing(fileName);

        Long fileId;

        try {
            fileId = processedFileRepository.saveProcessing(inputFile);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("El archivo ya fue registrado para procesamiento");
        }

        fileProcessingWorker.process(fileId, inputFile);

        return new ProcessFileResponse(
                fileId,
                inputFile.fileName(),
                "PROCESANDO",
                "Archivo aceptado para procesamiento",
                0,
                0,
                0);
    }
}
