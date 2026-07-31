package com.iroute.ibatch.domain.model;

public record TransactionRejection(
        int rejectionReasonId,
        String message) {
}
