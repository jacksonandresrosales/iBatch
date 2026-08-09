package com.iroute.ibatch.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.application.usecase.TransactionReprocessService;
import com.iroute.ibatch.dto.request.ReprocessTransactionRequest;
import com.iroute.ibatch.dto.response.ReprocessTransactionResponse;

@RestController
@RequestMapping("/transactions")
@Validated
public class TransactionController {

    private final TransactionReprocessService transactionReprocessService;

    public TransactionController(TransactionReprocessService transactionReprocessService) {
        this.transactionReprocessService = transactionReprocessService;
    }

    @PostMapping("/{id}")
    public ReprocessTransactionResponse reprocessTransaction(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @Valid @RequestBody ReprocessTransactionRequest request) {
        return transactionReprocessService.reprocessAmount(id, request.amount());
    }
}
