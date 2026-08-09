package com.iroute.ibatch.domain.rule.validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import com.iroute.ibatch.domain.model.TransactionRejection;

public class ValidationContext {

    private final CSVRecord record;
    private final Set<String> currentFileUniqueKeys;
    private final List<TransactionRejection> rejections = new ArrayList<>();

    private String account;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String processedUniqueKey;

    public ValidationContext(CSVRecord record, Set<String> currentFileUniqueKeys) {
        this.record = record;
        this.currentFileUniqueKeys = currentFileUniqueKeys;
    }

    public CSVRecord getRecord() {
        return record;
    }

    public Set<String> getCurrentFileUniqueKeys() {
        return currentFileUniqueKeys;
    }

    public List<TransactionRejection> getRejections() {
        return rejections;
    }

    public void addRejection(int code, String message) {
        rejections.add(new TransactionRejection(code, message));
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getProcessedUniqueKey() {
        return processedUniqueKey;
    }

    public void setProcessedUniqueKey(String processedUniqueKey) {
        this.processedUniqueKey = processedUniqueKey;
    }
}


