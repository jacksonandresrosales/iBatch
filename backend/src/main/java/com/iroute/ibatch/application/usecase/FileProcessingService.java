package com.iroute.ibatch.application.usecase;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iroute.ibatch.dto.response.ProcessFileResponse;
import com.iroute.ibatch.infrastructure.csv.CsvTransactionProcessor;
import com.iroute.ibatch.infrastructure.file.InputFileService;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@Service
public class FileProcessingService {

    private static final int FILE_STATUS_PROCESADO = 3;
    private static final int FILE_STATUS_PROCESADO_CON_RECHAZOS = 4;
    private static final int LOG_INFO = 1;
    private static final int LOG_SUCCESS = 2;
    private static final int EVENT_FILE_PROCESS_STARTED = 3;
    private static final int EVENT_FILE_PROCESS_FINISHED = 6;

    private final InputFileService inputFileService;
    private final ProcessedFileRepository processedFileRepository;
    private final CsvTransactionProcessor csvTransactionProcessor;
    private final ProcessingLogRepository processingLogRepository;

    public FileProcessingService(
            InputFileService inputFileService,
            ProcessedFileRepository processedFileRepository,
            CsvTransactionProcessor csvTransactionProcessor,
            ProcessingLogRepository processingLogRepository) {
        this.inputFileService = inputFileService;
        this.processedFileRepository = processedFileRepository;
        this.csvTransactionProcessor = csvTransactionProcessor;
        this.processingLogRepository = processingLogRepository;
    }

    @Transactional
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

        processingLogRepository.save(fileId, null, LOG_INFO, EVENT_FILE_PROCESS_STARTED, "Procesamiento de archivo iniciado");
        var result = csvTransactionProcessor.process(fileId, inputFile);
        var finalStatusId = result.rejectedCount() > 0
                ? FILE_STATUS_PROCESADO_CON_RECHAZOS
                : FILE_STATUS_PROCESADO;
        var finalStatus = result.rejectedCount() > 0
                ? "PROCESADO_CON_RECHAZOS"
                : "PROCESADO";

        processedFileRepository.updateFinished(
                fileId,
                finalStatusId,
                result.totalRecords(),
                result.processedCount(),
                result.rejectedCount());
        processingLogRepository.save(fileId, null, LOG_SUCCESS, EVENT_FILE_PROCESS_FINISHED, "Procesamiento de archivo finalizado");

        return new ProcessFileResponse(
                fileId,
                inputFile.fileName(),
                finalStatus,
                "Archivo procesado correctamente",
                result.totalRecords(),
                result.processedCount(),
                result.rejectedCount());
    }
}
