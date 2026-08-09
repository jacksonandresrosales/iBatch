package com.iroute.ibatch.domain.rule.validator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AmountValidator implements ValidationRule {

    private static final int MONTO_VACIO = 3;
    private static final int MONTO_INVALIDO = 4;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999999.99");

    @Override
    public void validate(ValidationContext context) {
        var rawAmount = context.getRecord().get("monto");
        
        if (rawAmount == null || rawAmount.isBlank()) {
            context.addRejection(MONTO_VACIO, "El monto es obligatorio");
            return;
        }

        try {
            var amount = new BigDecimal(rawAmount.trim()).setScale(2, RoundingMode.UNNECESSARY);

            if (amount.signum() <= 0) {
                context.addRejection(MONTO_INVALIDO, "El monto debe ser mayor a cero");
                return;
            }

            if (amount.compareTo(MAX_AMOUNT) > 0) {
                context.addRejection(MONTO_INVALIDO, "El monto excede el limite permitido");
                return;
            }

            context.setAmount(amount);
        } catch (ArithmeticException | NumberFormatException exception) {
            context.addRejection(MONTO_INVALIDO, "El monto debe ser un valor monetario valido");
        }
    }
}


