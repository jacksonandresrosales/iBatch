package com.iroute.ibatch.domain.model;

import java.time.LocalDateTime;

public record TransactionRejectionDetail(
        Long transactionRejectionId,
        Long transactionId,
        String reasonCode,
        String reasonName,
        String message,
        LocalDateTime createdAt) {
}
