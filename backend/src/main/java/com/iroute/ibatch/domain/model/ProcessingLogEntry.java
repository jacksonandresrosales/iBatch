package com.iroute.ibatch.domain.model;

import java.time.LocalDateTime;

public record ProcessingLogEntry(Long id, Long fileId, Long transactionId, String fileName,
        String level, String event, String message, LocalDateTime createdAt) {
}
