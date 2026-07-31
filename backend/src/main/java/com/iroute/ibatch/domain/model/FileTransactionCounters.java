package com.iroute.ibatch.domain.model;

public record FileTransactionCounters(
        int totalRecords,
        int processedCount,
        int rejectedCount) {
}
