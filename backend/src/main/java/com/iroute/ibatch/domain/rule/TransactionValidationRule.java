package com.iroute.ibatch.domain.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.TransactionRejection;
import com.iroute.ibatch.domain.model.ValidatedCsvTransaction;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@Component
public class TransactionValidationRule {

    private static final int STATUS_PROCESADO = 1;
    private static final int STATUS_RECHAZADA = 2;
    private static final int CUENTA_VACIA = 1;
    private static final int CUENTA_INVALIDA = 2;
    private static final int MONTO_VACIO = 3;
    private static final int MONTO_INVALIDO = 4;
    private static final int FECHA_VACIA = 5;
    private static final int FECHA_INVALIDA = 6;
    private static final int DUPLICADO = 7;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionRepository transactionRepository;

    public TransactionValidationRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public ValidatedCsvTransaction validate(CSVRecord record, Set<String> currentFileUniqueKeys) {
        var rejections = new ArrayList<TransactionRejection>();
        var rawAccount = record.get("cuenta");
        var rawAmount = record.get("monto");
        var rawDate = record.get("fecha");
        var account = validateAccount(rawAccount, rejections);
        var amount = validateAmount(rawAmount, rejections);
        var transactionDate = validateDate(rawDate, rejections);
        String processedUniqueKey = null;

        if (account != null && amount != null && transactionDate != null) {
            var candidateUniqueKey = buildUniqueKey(account, transactionDate, amount);

            if (currentFileUniqueKeys.contains(candidateUniqueKey)
                    || transactionRepository.existsProcessedUniqueKey(candidateUniqueKey)) {
                rejections.add(new TransactionRejection(DUPLICADO, "Ya existe una transaccion con la misma cuenta, fecha y monto"));
            } else {
                processedUniqueKey = candidateUniqueKey;
                currentFileUniqueKeys.add(candidateUniqueKey);
            }
        }

        var statusId = rejections.isEmpty() ? STATUS_PROCESADO : STATUS_RECHAZADA;
        var row = new CsvTransactionRow(
                Math.toIntExact(record.getRecordNumber() + 1),
                rawAccount,
                rawAmount,
                rawDate,
                account,
                amount,
                transactionDate,
                statusId,
                statusId == STATUS_PROCESADO ? processedUniqueKey : null);

        return new ValidatedCsvTransaction(row, rejections);
    }

    private String validateAccount(String rawAccount, ArrayList<TransactionRejection> rejections) {
        if (rawAccount == null || rawAccount.isBlank()) {
            rejections.add(new TransactionRejection(CUENTA_VACIA, "La cuenta es obligatoria"));
            return null;
        }

        var account = rawAccount.trim();

        if (!account.matches("\\d{10}")) {
            rejections.add(new TransactionRejection(CUENTA_INVALIDA, "La cuenta debe tener 10 digitos"));
            return null;
        }

        return account;
    }

    private BigDecimal validateAmount(String rawAmount, ArrayList<TransactionRejection> rejections) {
        if (rawAmount == null || rawAmount.isBlank()) {
            rejections.add(new TransactionRejection(MONTO_VACIO, "El monto es obligatorio"));
            return null;
        }

        try {
            return new BigDecimal(rawAmount.trim()).setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            rejections.add(new TransactionRejection(MONTO_INVALIDO, "El monto debe ser un valor monetario valido"));
            return null;
        }
    }

    private LocalDate validateDate(String rawDate, ArrayList<TransactionRejection> rejections) {
        if (rawDate == null || rawDate.isBlank()) {
            rejections.add(new TransactionRejection(FECHA_VACIA, "La fecha es obligatoria"));
            return null;
        }

        try {
            return LocalDate.parse(rawDate.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            rejections.add(new TransactionRejection(FECHA_INVALIDA, "La fecha debe cumplir el formato dd/MM/yyyy"));
            return null;
        }
    }

    private String buildUniqueKey(String account, LocalDate transactionDate, BigDecimal amount) {
        return account + "|" + transactionDate + "|" + amount.toPlainString();
    }
}
