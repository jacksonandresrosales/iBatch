package com.iroute.ibatch.dto.response;

import java.time.LocalDateTime;

public record TransactionRejectionResponse(
        Long transactionRejectionId,
        String reasonCode,
        String reasonName,
        String message,
        LocalDateTime createdAt) {
}
