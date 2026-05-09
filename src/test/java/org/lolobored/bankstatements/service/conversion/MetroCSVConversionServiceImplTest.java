package org.lolobored.bankstatements.service.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.MetroCSVConversionServiceImpl;

class MetroCSVConversionServiceImplTest {

  private MetroCSVConversionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MetroCSVConversionServiceImpl();
  }

  @Test
  void convertsDebitTransactionWhenMoneyInIsEmpty() throws ParseException {
    String csv =
        "Date,Description,Transaction Type,Money In,Money Out,Balance\n"
            + "01/03/2024,UBER EATS,PURCHASE,,12.99,1234.56\n";

    Statement statement = service.convertTableToTransactions("UK123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getCurrency()).isEqualTo("GBP");
    assertThat(statement.getTransactions()).hasSize(1);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
    assertThat(tx.getLabel()).isEqualTo("UBER EATS");
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void convertsDebitTransactionWhenMoneyInIsZero() throws ParseException {
    String csv =
        "Date,Description,Transaction Type,Money In,Money Out,Balance\n"
            + "01/03/2024,SHOP,PURCHASE,0.00,25.00,1000.00\n";

    Statement statement = service.convertTableToTransactions("UK123", Statement.DEBIT_ACCOUNT, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-25.00"));
  }

  @Test
  void convertsCreditTransaction() throws ParseException {
    String csv =
        "Date,Description,Transaction Type,Money In,Money Out,Balance\n"
            + "15/03/2024,SALARY CREDIT,CREDIT,5000.00,,6234.56\n";

    Statement statement = service.convertTableToTransactions("UK123", Statement.DEBIT_ACCOUNT, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    assertThat(tx.getLabel()).isEqualTo("SALARY CREDIT");
  }

  @Test
  void convertsMultipleTransactions() throws ParseException {
    String csv =
        "Date,Description,Transaction Type,Money In,Money Out,Balance\n"
            + "01/03/2024,UBER EATS,PURCHASE,,12.99,1234.56\n"
            + "02/03/2024,COFFEE,PURCHASE,,4.50,1230.06\n"
            + "03/03/2024,SALARY,CREDIT,5000.00,,6230.06\n";

    Statement statement = service.convertTableToTransactions("UK123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getTransactions()).hasSize(3);
    assertThat(statement.getTransactions().get(0).getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(statement.getTransactions().get(1).getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(statement.getTransactions().get(2).getType()).isEqualTo(Transaction.CREDIT_TYPE);
  }
}
