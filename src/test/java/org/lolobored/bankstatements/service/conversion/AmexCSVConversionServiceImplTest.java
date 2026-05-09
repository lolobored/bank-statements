package org.lolobored.bankstatements.service.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.AmexCSVConversionServiceImpl;

class AmexCSVConversionServiceImplTest {

  private AmexCSVConversionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AmexCSVConversionServiceImpl();
  }

  @Test
  void convertsDebitTransaction() {
    String csv =
        "Date,Description,Amount,Extended Details,Appears On Your Statement As,Address,Town/City,Postcode,Country,Reference\n"
            + "01/03/24,UBER EATS,12.99,Meal delivery,,,,,,'REF123'\n";

    Statement statement = service.convertTableToTransactions("123456", Statement.CREDIT_CARD, csv);

    assertThat(statement.getAccountNumber()).isEqualTo("123456");
    assertThat(statement.getAccountType()).isEqualTo(Statement.CREDIT_CARD);
    assertThat(statement.getCurrency()).isEqualTo("GBP");
    assertThat(statement.getTransactions()).hasSize(1);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
    assertThat(tx.getLabel()).isEqualTo("UBER EATS");
    assertThat(tx.getReference()).isEqualTo("REF123");
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2024, 3, 1));
    assertThat(tx.getAdditionalInformation()).isEqualTo("Meal delivery");
  }

  @Test
  void convertsCreditTransaction() {
    String csv =
        "Date,Description,Amount,Extended Details,Appears On Your Statement As,Address,Town/City,Postcode,Country,Reference\n"
            + "15/03/24,AMAZON REFUND,-5.00,Return,,,,,,REF456\n";

    Statement statement = service.convertTableToTransactions("123456", Statement.CREDIT_CARD, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
    assertThat(tx.getLabel()).isEqualTo("AMAZON REFUND");
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2024, 3, 15));
  }

  @Test
  void convertsMultipleTransactions() {
    String csv =
        "Date,Description,Amount,Extended Details,Appears On Your Statement As,Address,Town/City,Postcode,Country,Reference\n"
            + "01/03/24,UBER EATS,12.99,,,,,,,REF001\n"
            + "02/03/24,STARBUCKS,4.50,,,,,,,REF002\n"
            + "03/03/24,AMAZON REFUND,-20.00,,,,,,,REF003\n";

    Statement statement = service.convertTableToTransactions("123456", Statement.CREDIT_CARD, csv);

    assertThat(statement.getTransactions()).hasSize(3);
    assertThat(statement.getTransactions().get(0).getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(statement.getTransactions().get(1).getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(statement.getTransactions().get(2).getType()).isEqualTo(Transaction.CREDIT_TYPE);
  }

  @Test
  void stripsQuotesFromReference() {
    String csv =
        "Date,Description,Amount,Extended Details,Appears On Your Statement As,Address,Town/City,Postcode,Country,Reference\n"
            + "01/03/24,SHOP,10.00,,,,,,,'REF789'\n";

    Statement statement = service.convertTableToTransactions("123456", Statement.CREDIT_CARD, csv);

    assertThat(statement.getTransactions().get(0).getReference()).isEqualTo("REF789");
  }
}
