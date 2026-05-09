package org.lolobored.bankstatements.service.conversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.CommBankCSVConversionServiceImpl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;

class CommBankCSVConversionServiceImplTest {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private CommBankCSVConversionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommBankCSVConversionServiceImpl();
    }

    @Test
    void convertsDebitTransaction() throws ParseException {
        // CommBank CSV has no header row
        String csv = "01/03/2024,-12.99,UBER EATS,1234.56\n";

        Statement statement = service.convertTableToTransactions("987654", Statement.DEBIT_ACCOUNT, csv);

        assertThat(statement.getAccountNumber()).isEqualTo("987654");
        assertThat(statement.getCurrency()).isEqualTo("AUD");
        assertThat(statement.getTransactions()).hasSize(1);

        Transaction tx = statement.getTransactions().get(0);
        assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
        assertThat(tx.getLabel()).isEqualTo("UBER EATS");
        assertThat(tx.getDate()).isEqualTo(DATE_FORMAT.parse("01/03/2024"));
    }

    @Test
    void convertsCreditTransaction() throws ParseException {
        String csv = "15/03/2024,5000.00,SALARY CREDIT,6234.56\n";

        Statement statement = service.convertTableToTransactions("987654", Statement.DEBIT_ACCOUNT, csv);

        Transaction tx = statement.getTransactions().get(0);
        assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(tx.getLabel()).isEqualTo("SALARY CREDIT");
    }

    @Test
    void treatsZeroAmountAsCredit() throws ParseException {
        String csv = "01/03/2024,0.00,ZERO TX,100.00\n";

        Statement statement = service.convertTableToTransactions("987654", Statement.DEBIT_ACCOUNT, csv);

        assertThat(statement.getTransactions().get(0).getType()).isEqualTo(Transaction.CREDIT_TYPE);
    }

    @Test
    void convertsMultipleTransactions() throws ParseException {
        String csv = "01/03/2024,-12.99,UBER EATS,1234.56\n" +
                     "02/03/2024,-4.50,COFFEE,1230.06\n" +
                     "03/03/2024,5000.00,SALARY,6230.06\n";

        Statement statement = service.convertTableToTransactions("987654", Statement.DEBIT_ACCOUNT, csv);

        assertThat(statement.getTransactions()).hasSize(3);
        assertThat(statement.getTransactions().get(0).getType()).isEqualTo(Transaction.DEBIT_TYPE);
        assertThat(statement.getTransactions().get(2).getType()).isEqualTo(Transaction.CREDIT_TYPE);
    }
}
