package com.iroute.ibatch.domain.rule.validator;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AccountValidator implements ValidationRule {

    private static final int CUENTA_VACIA = 1;
    private static final int CUENTA_INVALIDA = 2;

    @Override
    public void validate(ValidationContext context) {
        var rawAccount = context.getRecord().get("cuenta");
        
        if (rawAccount == null || rawAccount.isBlank()) {
            context.addRejection(CUENTA_VACIA, "La cuenta es obligatoria");
            return;
        }

        var account = rawAccount.trim();

        if (!account.matches("\\d{10}")) {
            context.addRejection(CUENTA_INVALIDA, "La cuenta debe tener 10 digitos");
            return;
        }

        context.setAccount(account);
    }
}


