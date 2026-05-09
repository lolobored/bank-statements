package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.CommBankCsvLine;
import org.lolobored.bankstatements.service.conversion.CommBankCSVConversionService;
import org.springframework.stereotype.Service;

@Service
public class CommBankCSVConversionServiceImpl implements CommBankCSVConversionService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Override
  public Statement convertTableToTransactions(
      String accountNumber, String accountType, String csv) {

    Statement statement = new Statement();
    statement.setAccountNumber(accountNumber);
    statement.setAccountType(accountType);
    statement.setCurrency("AUD");

    List<CommBankCsvLine> commBankCSVLines = parseCSV(csv);
    for (CommBankCsvLine commBankCSVLine : commBankCSVLines) {
      Transaction transaction = new Transaction();
      transaction.setLabel(commBankCSVLine.getLabel());
      transaction.setDate(LocalDate.parse(commBankCSVLine.getDate(), DATE_FORMATTER));
      transaction.setAmount(new BigDecimal(commBankCSVLine.getAmount().trim()));
      if (transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
        transaction.setType(Transaction.CREDIT_TYPE);
      } else {
        transaction.setType(Transaction.DEBIT_TYPE);
      }

      statement.addTransaction(transaction);
    }
    return statement;
  }

  private List<CommBankCsvLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy<CommBankCsvLine> ms = new ColumnPositionMappingStrategy<>();
    ms.setType(CommBankCsvLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean<CommBankCsvLine> cb =
        new CsvToBeanBuilder<CommBankCsvLine>(reader)
            .withSeparator(',')
            .withType(CommBankCsvLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
