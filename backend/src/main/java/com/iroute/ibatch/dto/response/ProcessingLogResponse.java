package com.iroute.ibatch.dto.response;

import java.time.LocalDateTime;

public record ProcessingLogResponse(Long id, Long fileId, Long transactionId, String fileName,
        String level, String event, String message, LocalDateTime createdAt) {
}
