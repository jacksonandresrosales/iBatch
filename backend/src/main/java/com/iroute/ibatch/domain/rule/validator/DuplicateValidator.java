package com.iroute.ibatch.domain.rule.validator;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class DuplicateValidator implements ValidationRule {

    private static final int DUPLICADO = 7;

    @Override
    public void validate(ValidationContext context) {
        // Solo verificamos duplicados si las validaciones previas pasaron
        if (context.getAccount() == null || context.getAmount() == null || context.getTransactionDate() == null) {
            return;
        }

        var candidateUniqueKey = buildUniqueKey(context);

        if (context.getCurrentFileUniqueKeys().contains(candidateUniqueKey)) {
            context.addRejection(DUPLICADO, "Ya existe una transaccion con la misma cuenta, fecha y monto");
        } else {
            context.setProcessedUniqueKey(candidateUniqueKey);
            context.getCurrentFileUniqueKeys().add(candidateUniqueKey);
        }
    }

    private String buildUniqueKey(ValidationContext context) {
        return context.getAccount() + "|" + context.getTransactionDate() + "|" + context.getAmount().toPlainString();
    }
}


