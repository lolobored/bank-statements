package org.lolobored.bankstatements.service.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.conversion.impl.UOBXLSConversionServiceImpl;

class UOBXLSConversionServiceImplTest {

  @TempDir Path tempDir;

  private UOBXLSConversionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new UOBXLSConversionServiceImpl();
  }

  private File createXls(String[][] dataRows) throws IOException {
    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet = workbook.createSheet("Transactions");

    // Preamble rows before the header
    HSSFRow preamble = sheet.createRow(0);
    preamble.createCell(0).setCellValue("Account Statement");

    // Header row expected by the parser
    HSSFRow header = sheet.createRow(1);
    header.createCell(0).setCellValue("Transaction Date");
    header.createCell(1).setCellValue("Description");
    header.createCell(2).setCellValue("Withdrawal");
    header.createCell(3).setCellValue("Deposit");

    // Data rows
    for (int i = 0; i < dataRows.length; i++) {
      HSSFRow row = sheet.createRow(i + 2);
      row.createCell(0).setCellValue(dataRows[i][0]);
      row.createCell(1).setCellValue(dataRows[i][1]);
      row.createCell(2).setCellValue(Double.parseDouble(dataRows[i][2]));
      row.createCell(3).setCellValue(Double.parseDouble(dataRows[i][3]));
    }

    File file = tempDir.resolve("statement.xls").toFile();
    try (FileOutputStream out = new FileOutputStream(file)) {
      workbook.write(out);
    }
    workbook.close();
    return file;
  }

  @Test
  void convertsDebitTransaction() throws IOException {
    File xls = createXls(new String[][] {{"01 Mar 2024", "UBER EATS", "12.99", "0.0"}});

    Statement statement =
        service.convertTableToTransactions("SG456", Statement.DEBIT_ACCOUNT, xls.getAbsolutePath());

    assertThat(statement.getCurrency()).isEqualTo("SGD");
    assertThat(statement.getTransactions()).hasSize(1);

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("-12.99"));
    assertThat(tx.getLabel()).isEqualTo("UBER EATS");
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void convertsCreditTransaction() throws IOException {
    File xls = createXls(new String[][] {{"15 Mar 2024", "SALARY", "0.0", "5000.0"}});

    Statement statement =
        service.convertTableToTransactions("SG456", Statement.DEBIT_ACCOUNT, xls.getAbsolutePath());

    Transaction tx = statement.getTransactions().get(0);
    assertThat(tx.getType()).isEqualTo(Transaction.CREDIT_TYPE);
    assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5000.0"));
    assertThat(tx.getLabel()).isEqualTo("SALARY");
  }

  @Test
  void convertsMultipleTransactions() throws IOException {
    File xls =
        createXls(
            new String[][] {
              {"01 Mar 2024", "GRAB", "12.99", "0.0"},
              {"02 Mar 2024", "COFFEE", "4.50", "0.0"},
              {"15 Mar 2024", "SALARY", "0.0", "5000.0"}
            });

    Statement statement =
        service.convertTableToTransactions("SG456", Statement.DEBIT_ACCOUNT, xls.getAbsolutePath());

    assertThat(statement.getTransactions()).hasSize(3);
    assertThat(statement.getTransactions().get(0).getType()).isEqualTo(Transaction.DEBIT_TYPE);
    assertThat(statement.getTransactions().get(2).getType()).isEqualTo(Transaction.CREDIT_TYPE);
  }

  @Test
  void skipsRowsBeforeTransactionDateHeader() throws IOException {
    // The createXls helper puts a preamble row before the header — verify it's not parsed as a
    // transaction
    File xls = createXls(new String[][] {{"01 Mar 2024", "ONLY TX", "10.0", "0.0"}});

    Statement statement =
        service.convertTableToTransactions("SG456", Statement.DEBIT_ACCOUNT, xls.getAbsolutePath());

    assertThat(statement.getTransactions()).hasSize(1);
    assertThat(statement.getTransactions().get(0).getLabel()).isEqualTo("ONLY TX");
  }
}
