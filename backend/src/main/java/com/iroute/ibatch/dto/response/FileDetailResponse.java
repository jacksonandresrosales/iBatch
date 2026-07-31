package com.iroute.ibatch.dto.response;

import java.util.List;

public record FileDetailResponse(
        ProcessedFileResponse file,
        List<TransactionDetailResponse> transactions) {
}
