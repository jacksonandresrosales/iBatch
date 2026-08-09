package com.iroute.ibatch.application.usecase;

import org.springframework.stereotype.Service;

import com.iroute.ibatch.domain.model.ProcessingLogEntry;
import com.iroute.ibatch.dto.response.DashboardSummaryResponse;
import com.iroute.ibatch.dto.response.PageResponse;
import com.iroute.ibatch.dto.response.ProcessingLogResponse;
import com.iroute.ibatch.dto.response.RejectionReasonSummaryResponse;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@Service
public class DashboardService {

    private final ProcessedFileService processedFileService;
    private final TransactionRepository transactionRepository;
    private final ProcessingLogRepository processingLogRepository;

    public DashboardService(ProcessedFileService processedFileService,
            TransactionRepository transactionRepository, ProcessingLogRepository processingLogRepository) {
        this.processedFileService = processedFileService;
        this.transactionRepository = transactionRepository;
        this.processingLogRepository = processingLogRepository;
    }

    public DashboardSummaryResponse findSummary() {
        var files = processedFileService.findAll();
        int processed = files.stream().mapToInt(file -> file.processedTransactions()).sum();
        int rejected = files.stream().mapToInt(file -> file.rejectedTransactions()).sum();
        int total = processed + rejected;
        double rejectionRate = total == 0 ? 0D : rejected * 100D / total;
        return new DashboardSummaryResponse(
                files.size(), processed, rejected, rejectionRate,
                transactionRepository.findRejectionReasonSummary().stream()
                        .map(reason -> new RejectionReasonSummaryResponse(reason.code(), reason.name(), reason.count()))
                        .toList(),
                files.stream().limit(5).toList(),
                processingLogRepository.findRecentHighLevel(5).stream().map(this::toResponse).toList());
    }

    public PageResponse<ProcessingLogResponse> findRecentLogs(int page, int size) {
        var totalElements = processingLogRepository.countAllLogs();
        var content = processingLogRepository.findRecentPaginated(size, page * size).stream()
                .map(this::toResponse).toList();
        return PageResponse.of(content, page, size, totalElements);
    }

    private ProcessingLogResponse toResponse(ProcessingLogEntry log) {
        return new ProcessingLogResponse(log.id(), log.fileId(), log.transactionId(), log.fileName(),
                log.level(), log.event(), log.message(), log.createdAt());
    }
}
