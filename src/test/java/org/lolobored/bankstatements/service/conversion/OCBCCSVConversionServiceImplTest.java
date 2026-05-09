package org.lolobored.bankstatements.service.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.OCBCCSVConversionServiceImpl;

class OCBCCSVConversionServiceImplTest {

  // 6 header lines are skipped by the parser
  private static final String HEADER = "Line1\nLine2\nLine3\nLine4\nLine5\nLine6\n";

  private OCBCCSVConversionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new OCBCCSVConversionServiceImpl();
  }

  @Test
  void convertsFastPaymentAsTransfer() {
    String csv = HEADER + "01/03/2024,01/03/2024,\"FAST PAYMENT\nGRAB SINGAPORE\",12.99,\n";

    Statement statement = service.convertTableToTransactions("SG123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getCurrency()).isEqualTo("SGD");
    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.XFER_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
    assertThat(tx.getLabel()).isEqualTo("GRAB SINGAPORE");
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void convertsGiroSalaryAsCredit() {
    String csv = HEADER + "15/03/2024,15/03/2024,\"GIRO - SALARY\nMY COMPANY\",, 5000.00\n";

    Statement statement = service.convertTableToTransactions("SG123", Statement.DEBIT_ACCOUNT, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    assertThat(tx.getLabel()).isEqualTo("MY COMPANY");
  }

  @Test
  void convertsNetsQrAsDebit() {
    String csv = HEADER + "02/03/2024,02/03/2024,\"NETS QR\nCOFFEE SHOP\",4.50,\n";

    Statement statement = service.convertTableToTransactions("SG123", Statement.DEBIT_ACCOUNT, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-4.50"));
  }

  @Test
  void convertsInterestCreditAsCredit() {
    String csv = HEADER + "31/03/2024,31/03/2024,INTEREST CREDIT,,1.23\n";

    Statement statement = service.convertTableToTransactions("SG123", Statement.DEBIT_ACCOUNT, csv);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
    assertThat(tx.getLabel()).isEqualTo("Interests credit");
  }

  @Test
  void handlesThousandSeparatorInAmount() {
    String csv = HEADER + "01/03/2024,01/03/2024,\"GIRO - SALARY\nBIG CORP\",, \"1,500.00\"\n";

    Statement statement = service.convertTableToTransactions("SG123", Statement.DEBIT_ACCOUNT, csv);

    assertThat(statement.getTransactions().get(0).getAmount())
        .isEqualByComparingTo(new BigDecimal("1500.00"));
  }
}
