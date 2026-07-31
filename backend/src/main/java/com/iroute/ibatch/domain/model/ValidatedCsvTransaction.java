package com.iroute.ibatch.domain.model;

import java.util.List;

public record ValidatedCsvTransaction(
        CsvTransactionRow row,
        List<TransactionRejection> rejections) {
}
