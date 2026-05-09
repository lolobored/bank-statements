package org.lolobored.bankstatements.service.conversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.RevolutCSVConversionServiceImpl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;

class RevolutCSVConversionServiceImplTest {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private RevolutCSVConversionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RevolutCSVConversionServiceImpl();
    }

    @Test
    void extractsCurrencyFromHeader() throws ParseException {
        String csv = "Date;Reference;Paid Out (EUR);Paid In (EUR);Exchange Out;Exchange In;Balance;Category\n" +
                     "01 March 2024;UBER EATS;12.99;;;;1234.56;Transport\n";

        Statement statement = service.convertTableToTransactions("REV123", Statement.DEBIT_ACCOUNT, csv);

        assertThat(statement.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void convertsDebitTransaction() throws ParseException {
        String csv = "Date;Reference;Paid Out (GBP);Paid In (GBP);Exchange Out;Exchange In;Balance;Category\n" +
                     "01 March 2024;UBER EATS;12.99;;;;1234.56;Transport\n";

        Statement statement = service.convertTableToTransactions("REV123", Statement.DEBIT_ACCOUNT, csv);

        assertThat(statement.getTransactions()).hasSize(1);
        Transaction tx = statement.getTransactions().get(0);
        assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
        assertThat(tx.getLabel()).isEqualTo("UBER EATS");
        assertThat(tx.getAdditionalInformation()).isEqualTo("Transport");
        assertThat(tx.getDate()).isEqualTo(DATE_FORMAT.parse("01/03/2024"));
    }

    @Test
    void convertsCreditTransaction() throws ParseException {
        String csv = "Date;Reference;Paid Out (GBP);Paid In (GBP);Exchange Out;Exchange In;Balance;Category\n" +
                     "15 March 2024;SALARY;;5000.00;;;6234.56;Income\n";

        Statement statement = service.convertTableToTransactions("REV123", Statement.DEBIT_ACCOUNT, csv);

        Transaction tx = statement.getTransactions().get(0);
        assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(tx.getLabel()).isEqualTo("SALARY");
    }

    @Test
    void removesThousandSeparatorsFromAmount() throws ParseException {
        String csv = "Date;Reference;Paid Out (GBP);Paid In (GBP);Exchange Out;Exchange In;Balance;Category\n" +
                     "01 March 2024;BIG PAYMENT;1,234.56;;;;0;Other\n";

        Statement statement = service.convertTableToTransactions("REV123", Statement.DEBIT_ACCOUNT, csv);

        assertThat(statement.getTransactions().get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("-1234.56"));
    }

    @Test
    void parsesFullDateWithYear() throws ParseException {
        String csv = "Date;Reference;Paid Out (GBP);Paid In (GBP);Exchange Out;Exchange In;Balance;Category\n" +
                     "25 December 2023;GIFT;;100.00;;;100.00;Other\n";

        Statement statement = service.convertTableToTransactions("REV123", Statement.DEBIT_ACCOUNT, csv);

        assertThat(statement.getTransactions().get(0).getDate())
                .isEqualTo(DATE_FORMAT.parse("25/12/2023"));
    }
}
