package com.iroute.ibatch.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionDetailResponse(
        Long transactionId,
        int lineNumber,
        String rawAccount,
        String rawAmount,
        String rawDate,
        String account,
        BigDecimal amount,
        LocalDate transactionDate,
        String status,
        List<TransactionRejectionResponse> rejections,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
