package com.iroute.ibatch.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProcessedTransaction(
        Long transactionId,
        Long fileId,
        int lineNumber,
        String rawAccount,
        String rawAmount,
        String rawDate,
        String account,
        BigDecimal amount,
        LocalDate transactionDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
