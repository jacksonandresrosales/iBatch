package com.iroute.ibatch.application.usecase;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.iroute.ibatch.domain.model.ProcessedFile;
import com.iroute.ibatch.domain.model.ProcessedTransaction;
import com.iroute.ibatch.domain.model.TransactionRejectionDetail;
import com.iroute.ibatch.dto.response.FileDetailResponse;
import com.iroute.ibatch.dto.response.ProcessedFileResponse;
import com.iroute.ibatch.dto.response.TransactionDetailResponse;
import com.iroute.ibatch.dto.response.TransactionRejectionResponse;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@Service
public class ProcessedFileService {

    private final ProcessedFileRepository processedFileRepository;
    private final TransactionRepository transactionRepository;

    public ProcessedFileService(
            ProcessedFileRepository processedFileRepository,
            TransactionRepository transactionRepository) {
        this.processedFileRepository = processedFileRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<ProcessedFileResponse> findAll() {
        return processedFileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public FileDetailResponse findDetailById(Long fileId) {
        var file = processedFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("El archivo no existe"));
        var rejectionsByTransactionId = transactionRepository.findRejectionsByFileId(fileId).stream()
                .collect(Collectors.groupingBy(TransactionRejectionDetail::transactionId));
        var transactions = transactionRepository.findByFileId(fileId).stream()
                .map(transaction -> toTransactionResponse(transaction, rejectionsByTransactionId))
                .toList();

        return new FileDetailResponse(toResponse(file), transactions);
    }

    private ProcessedFileResponse toResponse(ProcessedFile processedFile) {
        return new ProcessedFileResponse(
                processedFile.id(),
                processedFile.fileName(),
                processedFile.status(),
                processedFile.totalTransactions(),
                processedFile.processedTransactions(),
                processedFile.rejectedTransactions(),
                processedFile.errorMessage(),
                processedFile.createdAt(),
                processedFile.updatedAt());
    }

    private TransactionDetailResponse toTransactionResponse(
            ProcessedTransaction transaction,
            Map<Long, List<TransactionRejectionDetail>> rejectionsByTransactionId) {
        var rejections = rejectionsByTransactionId
                .getOrDefault(transaction.transactionId(), List.of())
                .stream()
                .map(this::toRejectionResponse)
                .toList();

        return new TransactionDetailResponse(
                transaction.transactionId(),
                transaction.lineNumber(),
                transaction.rawAccount(),
                transaction.rawAmount(),
                transaction.rawDate(),
                transaction.account(),
                transaction.amount(),
                transaction.transactionDate(),
                transaction.status(),
                rejections,
                transaction.createdAt(),
                transaction.updatedAt());
    }

    private TransactionRejectionResponse toRejectionResponse(TransactionRejectionDetail rejection) {
        return new TransactionRejectionResponse(
                rejection.transactionRejectionId(),
                rejection.reasonCode(),
                rejection.reasonName(),
                rejection.message(),
                rejection.createdAt());
    }
}
