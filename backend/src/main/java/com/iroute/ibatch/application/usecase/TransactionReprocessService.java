package com.iroute.ibatch.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iroute.ibatch.domain.model.ProcessedTransaction;
import com.iroute.ibatch.domain.model.TransactionRejection;
import com.iroute.ibatch.domain.model.TransactionRejectionDetail;
import com.iroute.ibatch.dto.response.ReprocessTransactionResponse;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessedFileRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@Service
public class TransactionReprocessService {

    private static final int STATUS_PROCESADO = 1;
    private static final int STATUS_RECHAZADA = 2;
    private static final int FILE_STATUS_PROCESADO = 3;
    private static final int FILE_STATUS_PROCESADO_CON_RECHAZOS = 4;
    private static final int CUENTA_INVALIDA = 2;
    private static final int FECHA_INVALIDA = 6;
    private static final int DUPLICADO = 7;
    private static final int LOG_INFO = 1;
    private static final int LOG_SUCCESS = 2;
    private static final int EVENT_REPROCESS_STARTED = 7;
    private static final int EVENT_REPROCESS_FINISHED = 8;

    private final TransactionRepository transactionRepository;
    private final ProcessedFileRepository processedFileRepository;
    private final ProcessingLogRepository processingLogRepository;

    public TransactionReprocessService(
            TransactionRepository transactionRepository,
            ProcessedFileRepository processedFileRepository,
            ProcessingLogRepository processingLogRepository) {
        this.transactionRepository = transactionRepository;
        this.processedFileRepository = processedFileRepository;
        this.processingLogRepository = processingLogRepository;
    }

    @Transactional
    public ReprocessTransactionResponse reprocessAmount(
            Long transactionId,
            BigDecimal newAmount) {
        var transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("La transaccion no existe"));

        if (!"RECHAZADA".equals(transaction.status())) {
            throw new IllegalArgumentException("Solo se pueden reprocesar transacciones rechazadas");
        }

        var previousRejections = transactionRepository.findRejectionsByTransactionId(transactionId);
        var previousRejectionSummary = summarizeExistingRejections(previousRejections);
        var normalizedAmount = normalizeAmount(newAmount);
        var rejections = validateReprocess(transaction, normalizedAmount);
        var newStatusId = rejections.isEmpty() ? STATUS_PROCESADO : STATUS_RECHAZADA;
        var newStatus = rejections.isEmpty() ? "PROCESADO" : "RECHAZADA";
        var newUniqueKey = rejections.isEmpty()
                ? buildUniqueKey(transaction.account(), transaction.transactionDate(), normalizedAmount)
                : null;

        processingLogRepository.save(
                transaction.fileId(),
                transaction.transactionId(),
                LOG_INFO,
                EVENT_REPROCESS_STARTED,
                "Reproceso de transaccion iniciado");
        transactionRepository.updateReprocessedAmount(transactionId, normalizedAmount, newStatusId, newUniqueKey);
        transactionRepository.deleteRejections(transactionId);
        transactionRepository.saveRejections(transactionId, rejections);
        transactionRepository.saveReprocessHistory(
                transaction,
                normalizedAmount,
                newStatusId,
                previousRejectionSummary,
                summarizeNewRejections(rejections));
        updateFileCounters(transaction.fileId());
        processingLogRepository.save(
                transaction.fileId(),
                transaction.transactionId(),
                LOG_SUCCESS,
                EVENT_REPROCESS_FINISHED,
                "Reproceso de transaccion finalizado");

        return new ReprocessTransactionResponse(
                transactionId,
                transaction.fileId(),
                newStatus,
                "Transaccion reprocesada correctamente");
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }

        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("El monto debe tener maximo dos decimales");
        }
    }

    private List<TransactionRejection> validateReprocess(ProcessedTransaction transaction, BigDecimal newAmount) {
        var rejections = new ArrayList<TransactionRejection>();

        if (transaction.account() == null) {
            rejections.add(new TransactionRejection(
                    CUENTA_INVALIDA,
                    "La cuenta no es valida y no puede corregirse desde el reproceso de monto"));
        }

        if (transaction.transactionDate() == null) {
            rejections.add(new TransactionRejection(
                    FECHA_INVALIDA,
                    "La fecha no es valida y no puede corregirse desde el reproceso de monto"));
        }

        if (!rejections.isEmpty()) {
            return rejections;
        }

        var uniqueKey = buildUniqueKey(transaction.account(), transaction.transactionDate(), newAmount);

        if (transactionRepository.existsProcessedUniqueKeyExcludingTransaction(uniqueKey, transaction.transactionId())) {
            return List.of(new TransactionRejection(
                    DUPLICADO,
                    "Ya existe una transaccion con la misma cuenta, fecha y monto"));
        }

        return List.of();
    }

    private void updateFileCounters(Long fileId) {
        var counters = transactionRepository.countByFileId(fileId);
        var statusId = counters.rejectedCount() > 0
                ? FILE_STATUS_PROCESADO_CON_RECHAZOS
                : FILE_STATUS_PROCESADO;

        processedFileRepository.updateCounters(
                fileId,
                statusId,
                counters.totalRecords(),
                counters.processedCount(),
                counters.rejectedCount());
    }

    private String summarizeExistingRejections(List<TransactionRejectionDetail> rejections) {
        if (rejections.isEmpty()) {
            return null;
        }

        return rejections.stream()
                .map(rejection -> rejection.reasonCode() + ": " + rejection.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse(null);
    }

    private String summarizeNewRejections(List<TransactionRejection> rejections) {
        if (rejections.isEmpty()) {
            return null;
        }

        return rejections.stream()
                .map(rejection -> rejection.rejectionReasonId() + ": " + rejection.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse(null);
    }

    private String buildUniqueKey(String account, java.time.LocalDate transactionDate, BigDecimal amount) {
        return account + "|" + transactionDate + "|" + amount.toPlainString();
    }
}
