package com.iroute.ibatch.dto.response;

import java.util.List;

public record FileDetailResponse(
        ProcessedFileResponse file,
        List<TransactionDetailResponse> transactions,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public FileDetailResponse(ProcessedFileResponse file, List<TransactionDetailResponse> transactions) {
        this(file, transactions, 0, transactions.size(), transactions.size(), transactions.isEmpty() ? 0 : 1);
    }
}
