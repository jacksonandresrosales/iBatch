package com.iroute.ibatch.domain.model;

public record PersistedTransactionRejection(
        Long transactionId,
        TransactionRejection rejection) {
}


