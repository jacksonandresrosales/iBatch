package com.iroute.ibatch.dto.response;

import java.time.LocalDateTime;

public record ProcessedFileResponse(
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
