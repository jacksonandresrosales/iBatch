package com.iroute.ibatch.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.infrastructure.csv.CsvTransactionProcessor;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;

@Service
public class FileProcessingWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileProcessingWorker.class);
    private static final int FILE_STATUS_PROCESADO = 3;
    private static final int FILE_STATUS_PROCESADO_CON_RECHAZOS = 4;
    private static final int LOG_INFO = 1;
    private static final int LOG_SUCCESS = 2;
    private static final int LOG_ERROR = 4;
    private static final int EVENT_FILE_PROCESS_STARTED = 3;
    private static final int EVENT_FILE_PROCESS_FINISHED = 6;
    private static final int EVENT_PROCESS_ERROR = 9;

    private final CsvTransactionProcessor csvTransactionProcessor;
    private final ProcessedFileRepository processedFileRepository;
    private final ProcessingLogRepository processingLogRepository;
    private final FileProgressTracker fileProgressTracker;

    public FileProcessingWorker(
            CsvTransactionProcessor csvTransactionProcessor,
            ProcessedFileRepository processedFileRepository,
            ProcessingLogRepository processingLogRepository,
            FileProgressTracker fileProgressTracker) {
        this.csvTransactionProcessor = csvTransactionProcessor;
        this.processedFileRepository = processedFileRepository;
        this.processingLogRepository = processingLogRepository;
        this.fileProgressTracker = fileProgressTracker;
    }

    @Async("fileProcessingExecutor")
    public void process(Long fileId, InputFileMetadata inputFile) {
        try {
            fileProgressTracker.startProgress(fileId, inputFile.fileName(), 0);
            processingLogRepository.save(fileId, null, LOG_INFO, EVENT_FILE_PROCESS_STARTED,
                    "Procesamiento de archivo iniciado");

            var result = csvTransactionProcessor.process(fileId, inputFile, fileProgressTracker);
            var finalStatusId = result.rejectedCount() > 0
                    ? FILE_STATUS_PROCESADO_CON_RECHAZOS
                    : FILE_STATUS_PROCESADO;
            processedFileRepository.updateFinished(fileId, finalStatusId, result.totalRecords(),
                    result.processedCount(), result.rejectedCount());
            processingLogRepository.save(fileId, null, LOG_SUCCESS, EVENT_FILE_PROCESS_FINISHED,
                    "Procesamiento de archivo finalizado");
            fileProgressTracker.finishProgress(fileId, inputFile.fileName(), result.processedCount(),
                    result.rejectedCount(), result.totalRecords());
        } catch (RuntimeException exception) {
            LOGGER.error("Fallo el procesamiento del archivo {} con id {}", inputFile.fileName(), fileId, exception);
            processedFileRepository.updateError(fileId);
            processingLogRepository.save(fileId, null, LOG_ERROR, EVENT_PROCESS_ERROR,
                    "El procesamiento del archivo fallo");
            fileProgressTracker.errorProgress(fileId, inputFile.fileName());
        }
    }
}
