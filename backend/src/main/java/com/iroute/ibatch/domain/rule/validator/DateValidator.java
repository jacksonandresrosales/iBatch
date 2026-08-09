package com.iroute.ibatch.domain.rule.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class DateValidator implements ValidationRule {

    private static final int FECHA_VACIA = 5;
    private static final int FECHA_INVALIDA = 6;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    public void validate(ValidationContext context) {
        var rawDate = context.getRecord().get("fecha");
        
        if (rawDate == null || rawDate.isBlank()) {
            context.addRejection(FECHA_VACIA, "La fecha es obligatoria");
            return;
        }

        try {
            var transactionDate = LocalDate.parse(rawDate.trim(), DATE_FORMATTER);
            context.setTransactionDate(transactionDate);
        } catch (DateTimeParseException exception) {
            context.addRejection(FECHA_INVALIDA, "La fecha debe cumplir el formato dd/MM/yyyy");
        }
    }
}


