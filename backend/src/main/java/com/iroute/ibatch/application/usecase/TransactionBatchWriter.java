package com.iroute.ibatch.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.PersistedTransactionRejection;
import com.iroute.ibatch.domain.model.TransactionBatchResult;
import com.iroute.ibatch.domain.model.TransactionRejection;
import com.iroute.ibatch.domain.model.ValidatedCsvTransaction;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@Service
public class TransactionBatchWriter {

    private static final int STATUS_RECHAZADA = 2;
    private static final int DUPLICADO = 7;
    private static final String DUPLICATE_MESSAGE = "Ya existe una transaccion con la misma cuenta, fecha y monto";

    private final TransactionRepository transactionRepository;
    private final ProcessingLogRepository processingLogRepository;

    public TransactionBatchWriter(
            TransactionRepository transactionRepository,
            ProcessingLogRepository processingLogRepository) {
        this.transactionRepository = transactionRepository;
        this.processingLogRepository = processingLogRepository;
    }

    @Transactional
    public TransactionBatchResult write(Long fileId, List<ValidatedCsvTransaction> transactions) {
        var candidateKeys = transactions.stream()
                .filter(transaction -> transaction.rejections().isEmpty())
                .map(transaction -> transaction.row().processedUniqueKey())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var existingKeys = transactionRepository.findExistingProcessedUniqueKeys(candidateKeys);
        var normalized = transactions.stream()
                .map(transaction -> transaction.row().processedUniqueKey() != null
                        && existingKeys.contains(transaction.row().processedUniqueKey())
                        ? toDuplicate(transaction)
                        : transaction)
                .toList();
        var rejected = normalized.stream()
                .filter(transaction -> !transaction.rejections().isEmpty())
                .toList();
        var accepted = normalized.stream()
                .filter(transaction -> transaction.rejections().isEmpty())
                .toList();

        transactionRepository.saveBatch(fileId, rejected.stream().map(ValidatedCsvTransaction::row).toList(), false);
        transactionRepository.saveBatch(fileId, accepted.stream().map(ValidatedCsvTransaction::row).toList(), true);

        var lineNumbers = normalized.stream().map(transaction -> transaction.row().lineNumber()).toList();
        var initialTransactionIds = transactionRepository.findIdsByFileAndLineNumbers(fileId, lineNumbers);
        var concurrentDuplicates = accepted.stream()
                .filter(transaction -> !initialTransactionIds.containsKey(transaction.row().lineNumber()))
                .map(this::toDuplicate)
                .toList();

        if (!concurrentDuplicates.isEmpty()) {
            transactionRepository.saveBatch(
                    fileId,
                    concurrentDuplicates.stream().map(ValidatedCsvTransaction::row).toList(),
                    false);
        }
        var transactionIds = concurrentDuplicates.isEmpty()
                ? initialTransactionIds
                : transactionRepository.findIdsByFileAndLineNumbers(fileId, lineNumbers);

        var persistedRejections = new ArrayList<PersistedTransactionRejection>();
        var allRejected = new ArrayList<>(rejected);
        allRejected.addAll(concurrentDuplicates);

        for (var transaction : allRejected) {
            var transactionId = transactionIds.get(transaction.row().lineNumber());
            for (var rejection : transaction.rejections()) {
                persistedRejections.add(new PersistedTransactionRejection(transactionId, rejection));
            }
        }

        transactionRepository.saveRejectionsBatch(persistedRejections);
        processingLogRepository.saveRejectedRowsBatch(
                fileId,
                allRejected.stream()
                        .map(transaction -> transactionIds.get(transaction.row().lineNumber()))
                        .toList());

        return new TransactionBatchResult(
                accepted.size() - concurrentDuplicates.size(),
                allRejected.size());
    }

    private ValidatedCsvTransaction toDuplicate(ValidatedCsvTransaction transaction) {
        return new ValidatedCsvTransaction(
                toRejectedRow(transaction.row()),
                List.of(new TransactionRejection(DUPLICADO, DUPLICATE_MESSAGE)));
    }

    private CsvTransactionRow toRejectedRow(CsvTransactionRow row) {
        return new CsvTransactionRow(
                row.lineNumber(),
                row.rawAccount(),
                row.rawAmount(),
                row.rawDate(),
                row.account(),
                row.amount(),
                row.transactionDate(),
                STATUS_RECHAZADA,
                null);
    }
}


