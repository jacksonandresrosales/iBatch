package com.iroute.ibatch.dto.response;

import java.util.List;

public record DashboardSummaryResponse(int totalFiles, int totalProcessedTransactions,
        int totalRejectedTransactions, double rejectionRate,
        List<RejectionReasonSummaryResponse> rejectionReasons,
        List<ProcessedFileResponse> recentFiles,
        List<ProcessingLogResponse> recentEvents) {
}
