package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.CommBankCsvLine;
import org.lolobored.bankstatements.service.conversion.CommBankCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class CommBankCSVConversionServiceImpl implements CommBankCSVConversionService {

  private static SimpleDateFormat commBankCSVDate = new SimpleDateFormat("dd/MM/yyyy");

  @Override
  public Statement convertCSVToTransactions(String accountNumber, String accountType, String csv) throws ParseException {

    Statement statement = new Statement();
    statement.setAccountNumber(accountNumber);
    statement.setAccountType(accountType);
    statement.setCurrency("AUD");

    List<CommBankCsvLine> commBankCSVLines = parseCSV(csv);
    for (CommBankCsvLine commBankCSVLine : commBankCSVLines) {
      Transaction transaction = new Transaction();
      transaction.setLabel(commBankCSVLine.getLabel());
      transaction.setDate(commBankCSVDate.parse(commBankCSVLine.getDate()));
      transaction.setAmount(new BigDecimal(commBankCSVLine.getAmount().trim()));
      statement.addTransaction(transaction);
    }
    return statement;
  }

  private List<CommBankCsvLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
    ms.setType(CommBankCsvLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean cb = new CsvToBeanBuilder(reader)
            .withSeparator(',')
            .withType(CommBankCsvLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
