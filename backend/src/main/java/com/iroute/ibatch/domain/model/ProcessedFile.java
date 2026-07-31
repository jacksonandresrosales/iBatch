package com.iroute.ibatch.domain.model;

import java.time.LocalDateTime;

public record ProcessedFile(
        Long id,
        String fileName,
        String status,
        int totalTransactions,
        int processedTransactions,
        int rejectedTransactions,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
