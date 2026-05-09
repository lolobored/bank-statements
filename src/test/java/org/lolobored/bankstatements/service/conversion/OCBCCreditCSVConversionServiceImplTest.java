package org.lolobored.bankstatements.service.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.OCBCCreditCSVConversionServiceImpl;

class OCBCCreditCSVConversionServiceImplTest {

  // 7 header lines are skipped by the parser
  private static final String HEADER = "Line1\nLine2\nLine3\nLine4\nLine5\nLine6\nLine7\n";

  private OCBCCreditCSVConversionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new OCBCCreditCSVConversionServiceImpl();
  }

  @Test
  void convertsDebitPurchase() throws ParseException {
    // OCBCCredit CSV: date, description, debitAmount, creditAmount
    String csv = HEADER + "01/03/2024,\"DEBIT PURCHASE\nxx-1234 GRAB SINGAPORE 01/03/24\",12.99,\n";

    Statement statement = service.convertTableToTransactions("CC123", Statement.CREDIT_CARD, csv);

    assertThat(statement.getCurrency()).isEqualTo("SGD");
    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void convertsCashRebateAsCredit() throws ParseException {
    String csv = HEADER + "15/03/2024,\"CASH REBATE\nMONTHLY REBATE\",, 25.00\n";

    Statement statement = service.convertTableToTransactions("CC123", Statement.CREDIT_CARD, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
    assertThat(tx.getLabel()).isEqualTo("MONTHLY REBATE");
  }

  @Test
  void convertsFastPaymentAsTransfer() throws ParseException {
    String csv = HEADER + "02/03/2024,\"FAST PAYMENT\nSHOP ABC\",50.00,\n";

    Statement statement = service.convertTableToTransactions("CC123", Statement.CREDIT_CARD, csv);

    assertThat(statement.getTransactions().get(0).getType()).isEqualTo(Transaction.XFER_TYPE);
  }

  @Test
  void convertsInterestCreditLabel() throws ParseException {
    String csv = HEADER + "31/03/2024,INTEREST CREDIT,,1.00\n";

    Statement statement = service.convertTableToTransactions("CC123", Statement.CREDIT_CARD, csv);

    assertThat(statement.getTransactions().get(0).getLabel()).isEqualTo("Interests credit");
  }
}
