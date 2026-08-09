package com.iroute.ibatch.domain.model;

public record FileProgress(
        Long fileId,
        String fileName,
        int processedCount,
        int rejectedCount,
        int totalRecords,
        double percentage,
        String status,
        boolean completed,
        boolean error) {
}
