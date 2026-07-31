package com.iroute.ibatch.dto.response;

public record ReprocessTransactionResponse(
        Long transactionId,
        Long fileId,
        String status,
        String message) {
}
