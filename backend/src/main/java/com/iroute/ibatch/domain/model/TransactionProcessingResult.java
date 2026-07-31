package com.iroute.ibatch.domain.model;

public record TransactionProcessingResult(
        int totalRecords,
        int processedCount,
        int rejectedCount) {
}
