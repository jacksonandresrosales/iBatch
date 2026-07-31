package com.iroute.ibatch.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CsvTransactionRow(
        int lineNumber,
        String rawAccount,
        String rawAmount,
        String rawDate,
        String account,
        BigDecimal amount,
        LocalDate transactionDate,
        int transactionStatusId,
        String processedUniqueKey) {
}
