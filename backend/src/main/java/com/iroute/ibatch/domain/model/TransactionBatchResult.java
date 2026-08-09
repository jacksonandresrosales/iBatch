package com.iroute.ibatch.domain.model;

public record TransactionBatchResult(
        int processedCount,
        int rejectedCount) {
}


