package com.iroute.ibatch.domain.rule;

import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import com.iroute.ibatch.domain.model.CsvTransactionRow;
import com.iroute.ibatch.domain.model.ValidatedCsvTransaction;
import com.iroute.ibatch.domain.rule.validator.ValidationContext;
import com.iroute.ibatch.domain.rule.validator.ValidationRule;

@Component
public class TransactionValidationRule {

    private static final int STATUS_PROCESADO = 1;
    private static final int STATUS_RECHAZADA = 2;

    private final List<ValidationRule> validationRules;

    public TransactionValidationRule(List<ValidationRule> validationRules) {
        this.validationRules = validationRules;
    }

    public ValidatedCsvTransaction validate(CSVRecord record, Set<String> currentFileUniqueKeys) {
        var context = new ValidationContext(record, currentFileUniqueKeys);

        for (ValidationRule rule : validationRules) {
            rule.validate(context);
        }

        var statusId = context.getRejections().isEmpty() ? STATUS_PROCESADO : STATUS_RECHAZADA;

        var row = new CsvTransactionRow(
                Math.toIntExact(record.getRecordNumber() + 1),
                record.get("cuenta"),
                record.get("monto"),
                record.get("fecha"),
                context.getAccount(),
                context.getAmount(),
                context.getTransactionDate(),
                statusId,
                statusId == STATUS_PROCESADO ? context.getProcessedUniqueKey() : null);

        return new ValidatedCsvTransaction(row, context.getRejections());
    }
}
