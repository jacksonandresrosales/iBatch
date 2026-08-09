package com.iroute.ibatch.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.HashSet;

import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;

class TransactionValidationRuleTest {

    private final TransactionValidationRule rule = new TransactionValidationRule(List.of(
            new com.iroute.ibatch.domain.rule.validator.AccountValidator(),
            new com.iroute.ibatch.domain.rule.validator.AmountValidator(),
            new com.iroute.ibatch.domain.rule.validator.DateValidator(),
            new com.iroute.ibatch.domain.rule.validator.DuplicateValidator()
    ));

    @Test
    void shouldProcessValidTransaction() throws Exception {
        var record = parseRecord("cuenta,monto,fecha\n2000000000,3241.71,31/07/2026");
        var result = rule.validate(record, new HashSet<>());

        assertThat(result.row().transactionStatusId()).isEqualTo(1);
        assertThat(result.row().processedUniqueKey()).isEqualTo("2000000000|2026-07-31|3241.71");
        assertThat(result.rejections()).isEmpty();
    }

    @Test
    void shouldRejectInvalidFields() throws Exception {
        var result = rule.validate(
                parseRecord("cuenta,monto,fecha\nabc,xyz,2026-07-31"),
                new HashSet<>());

        assertThat(result.row().transactionStatusId()).isEqualTo(2);
        assertThat(result.row().processedUniqueKey()).isNull();
        assertThat(result.rejections()).extracting("rejectionReasonId").containsExactly(2, 4, 6);
    }

    @Test
    void shouldRejectZeroAndNegativeAmounts() throws Exception {
        var zeroAmountResult = rule.validate(
                parseRecord("cuenta,monto,fecha\n2000000000,0,31/07/2026"),
                new HashSet<>());
        var negativeAmountResult = rule.validate(
                parseRecord("cuenta,monto,fecha\n2000000000,-12.50,31/07/2026"),
                new HashSet<>());

        assertThat(zeroAmountResult.rejections()).extracting("rejectionReasonId").containsExactly(4);
        assertThat(negativeAmountResult.rejections()).extracting("rejectionReasonId").containsExactly(4);
    }

    @Test
    void shouldRejectInvalidCalendarDateAndAmountOutsideDatabaseRange() throws Exception {
        var invalidDateResult = rule.validate(
                parseRecord("cuenta,monto,fecha\n2000000000,1.00,31/02/2026"),
                new HashSet<>());
        var oversizedAmountResult = rule.validate(
                parseRecord("cuenta,monto,fecha\n2000000000,10000000000000000.00,31/07/2026"),
                new HashSet<>());

        assertThat(invalidDateResult.rejections()).extracting("rejectionReasonId").containsExactly(6);
        assertThat(oversizedAmountResult.rejections()).extracting("rejectionReasonId").containsExactly(4);
    }

    @Test
    void shouldRejectDuplicateInsideSameFile() throws Exception {
        var record = parseRecord("cuenta,monto,fecha\n2000000000,3241.71,31/07/2026");
        var currentFileUniqueKeys = new HashSet<String>();
        currentFileUniqueKeys.add("2000000000|2026-07-31|3241.71");
        var result = rule.validate(record, currentFileUniqueKeys);

        assertThat(result.row().transactionStatusId()).isEqualTo(2);
        assertThat(result.rejections()).extracting("rejectionReasonId").containsExactly(7);
    }

    private org.apache.commons.csv.CSVRecord parseRecord(String csv) throws Exception {
        var csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        try (var parser = csvFormat.parse(new StringReader(csv))) {
            return parser.iterator().next();
        }
    }
}
