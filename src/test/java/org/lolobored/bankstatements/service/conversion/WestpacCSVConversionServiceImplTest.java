package org.lolobored.bankstatements.service.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.WestpacCSVConversionServiceImpl;

class WestpacCSVConversionServiceImplTest {

  private WestpacCSVConversionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new WestpacCSVConversionServiceImpl();
  }

  @Test
  void convertsDebitTransaction() throws ParseException {
    String csv =
        "Bank Account,Date,Narrative,Debit Amount,Credit Amount,Balance,Categories,Serial\n"
            + "BSB-123,01/03/2024,UBER EATS,12.99,,1234.56,,\n";

    Statement statement = service.convertTableToTransactions("AU123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getCurrency()).isEqualTo("AUD");
    assertThat(statement.getTransactions()).hasSize(1);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
    assertThat(tx.getLabel()).isEqualTo("UBER EATS");
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void convertsCreditTransaction() throws ParseException {
    String csv =
        "Bank Account,Date,Narrative,Debit Amount,Credit Amount,Balance,Categories,Serial\n"
            + "BSB-123,15/03/2024,SALARY,,5000.00,6234.56,,\n";

    Statement statement = service.convertTableToTransactions("AU123", Statement.DEBIT_ACCOUNT, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    assertThat(tx.getLabel()).isEqualTo("SALARY");
  }

  @Test
  void stripsNewlinesFromNarrative() throws ParseException {
    String csv =
        "Bank Account,Date,Narrative,Debit Amount,Credit Amount,Balance,Categories,Serial\n"
            + "BSB-123,01/03/2024,\"UBER\r\nEATS\",12.99,,1234.56,,\n";

    Statement statement = service.convertTableToTransactions("AU123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getTransactions().get(0).getLabel()).isEqualTo("UBER EATS");
  }

  @Test
  void handlesThousandSeparatorInAmount() throws ParseException {
    // Amounts with thousand separators are quoted in the real Westpac CSV export
    String csv =
        "Bank Account,Date,Narrative,Debit Amount,Credit Amount,Balance,Categories,Serial\n"
            + "BSB-123,01/03/2024,SALARY,,\"1,500.00\",6234.56,,\n";

    Statement statement = service.convertTableToTransactions("AU123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getTransactions().get(0).getAmount())
        .isEqualByComparingTo(new BigDecimal("1500.00"));
  }

  @Test
  void convertsMultipleTransactions() throws ParseException {
    String csv =
        "Bank Account,Date,Narrative,Debit Amount,Credit Amount,Balance,Categories,Serial\n"
            + "BSB-123,01/03/2024,UBER EATS,12.99,,1234.56,,\n"
            + "BSB-123,02/03/2024,COFFEE,4.50,,1230.06,,\n"
            + "BSB-123,03/03/2024,SALARY,,5000.00,6230.06,,\n";

    Statement statement = service.convertTableToTransactions("AU123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getTransactions()).hasSize(3);
    assertThat(statement.getTransactions().get(0).getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(statement.getTransactions().get(2).getType()).isEqualTo(Transaction.CREDIT_TYPE);
  }
}
