package com.iroute.ibatch.application.usecase;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.iroute.ibatch.domain.model.FileProgress;

@Component
public class FileProgressTracker {

    private final Map<Long, FileProgress> progressByFile = new ConcurrentHashMap<>();

    public void startProgress(Long fileId, String fileName, int totalRecords) {
        progressByFile.put(fileId, new FileProgress(
                fileId, fileName, 0, 0, totalRecords, 0.0, "PROCESANDO", false, false));
    }

    public void updateProgress(Long fileId, String fileName, int processedCount, int rejectedCount, int totalRecords) {
        int completedRecords = processedCount + rejectedCount;
        double percentage = totalRecords > 0
                ? Math.min(100.0, Math.round((double) completedRecords / totalRecords * 1000.0) / 10.0)
                : 0.0;
        progressByFile.put(fileId, new FileProgress(
                fileId, fileName, processedCount, rejectedCount, totalRecords,
                percentage, "PROCESANDO", false, false));
    }

    public void finishProgress(Long fileId, String fileName, int processedCount, int rejectedCount, int totalRecords) {
        progressByFile.put(fileId, new FileProgress(
                fileId, fileName, processedCount, rejectedCount, totalRecords, 100.0,
                rejectedCount > 0 ? "PROCESADO_CON_RECHAZOS" : "PROCESADO", true, false));
    }

    public void errorProgress(Long fileId, String fileName) {
        progressByFile.put(fileId, new FileProgress(
                fileId, fileName, 0, 0, 0, 0.0, "ERROR", true, true));
    }

    public Optional<FileProgress> getProgress(Long fileId) {
        return Optional.ofNullable(progressByFile.get(fileId));
    }
}
