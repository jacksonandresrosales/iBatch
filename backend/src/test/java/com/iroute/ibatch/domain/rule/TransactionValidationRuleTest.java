package com.iroute.ibatch.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.HashSet;

import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionValidationRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void shouldProcessValidTransaction() throws Exception {
        var record = parseRecord("cuenta,monto,fecha\n2000000000,3241.71,31/07/2026");
        var currentFileUniqueKeys = new HashSet<String>();

        when(transactionRepository.existsProcessedUniqueKey("2000000000|2026-07-31|3241.71")).thenReturn(false);

        var rule = new TransactionValidationRule(transactionRepository);
        var result = rule.validate(record, currentFileUniqueKeys);

        assertThat(result.row().transactionStatusId()).isEqualTo(1);
        assertThat(result.row().processedUniqueKey()).isEqualTo("2000000000|2026-07-31|3241.71");
        assertThat(result.rejections()).isEmpty();
    }

    @Test
    void shouldRejectInvalidFields() throws Exception {
        var record = parseRecord("cuenta,monto,fecha\nabc,xyz,2026-07-31");
        var rule = new TransactionValidationRule(transactionRepository);

        var result = rule.validate(record, new HashSet<>());

        assertThat(result.row().transactionStatusId()).isEqualTo(2);
        assertThat(result.row().processedUniqueKey()).isNull();
        assertThat(result.rejections())
                .extracting("rejectionReasonId")
                .containsExactly(2, 4, 6);
    }

    @Test
    void shouldRejectZeroAndNegativeAmounts() throws Exception {
        var rule = new TransactionValidationRule(transactionRepository);

        var zeroAmountResult = rule.validate(
                parseRecord("cuenta,monto,fecha\n2000000000,0,31/07/2026"),
                new HashSet<>());
        var negativeAmountResult = rule.validate(
                parseRecord("cuenta,monto,fecha\n2000000000,-12.50,31/07/2026"),
                new HashSet<>());

        assertThat(zeroAmountResult.rejections())
                .extracting("rejectionReasonId")
                .containsExactly(4);
        assertThat(negativeAmountResult.rejections())
                .extracting("rejectionReasonId")
                .containsExactly(4);
    }

    @Test
    void shouldRejectDuplicateInsideSameFile() throws Exception {
        var record = parseRecord("cuenta,monto,fecha\n2000000000,3241.71,31/07/2026");
        var currentFileUniqueKeys = new HashSet<String>();
        currentFileUniqueKeys.add("2000000000|2026-07-31|3241.71");

        var rule = new TransactionValidationRule(transactionRepository);
        var result = rule.validate(record, currentFileUniqueKeys);

        assertThat(result.row().transactionStatusId()).isEqualTo(2);
        assertThat(result.rejections())
                .extracting("rejectionReasonId")
                .containsExactly(7);
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
