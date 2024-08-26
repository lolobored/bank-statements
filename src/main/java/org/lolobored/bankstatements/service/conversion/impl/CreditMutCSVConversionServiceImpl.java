package org.lolobored.bankstatements.service.conversion.impl;


import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.CreditMutCsvLine;
import org.lolobored.bankstatements.service.conversion.CreditMutCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class CreditMutCSVConversionServiceImpl implements CreditMutCSVConversionService {

  private static SimpleDateFormat amexCSVDate = new SimpleDateFormat("dd/MM/yyyy");

  @Override
  public Statement convertTableToTransactions(String accountNumber, String accountType, String csv) throws ParseException {

    Statement statement = new Statement();
    statement.setAccountNumber(accountNumber);
    statement.setAccountType(accountType);
    statement.setCurrency("GBP");

    List<CreditMutCsvLine> creditMutCsvLines = parseCSV(csv);
    for (CreditMutCsvLine creditMutCsvLine : creditMutCsvLines) {
      Transaction transaction = new Transaction();
      transaction.setLabel(creditMutCsvLine.getLabel());
      transaction.setDate(amexCSVDate.parse(creditMutCsvLine.getValueDate()));
      if (StringUtils.isEmpty(creditMutCsvLine.getMoneyIn())) {
        transaction.setAmount(new BigDecimal(creditMutCsvLine.getMoneyOut()));
        transaction.setType(Transaction.DEBIT_TYPE);
      } else {
        transaction.setAmount(new BigDecimal(creditMutCsvLine.getMoneyIn()));
        transaction.setType(Transaction.CREDIT_TYPE);
      }
      statement.addTransaction(transaction);
    }
    return statement;
  }

  private List<CreditMutCsvLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
    ms.setType(CreditMutCsvLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean cb = new CsvToBeanBuilder(reader)
            .withSeparator(';')
            .withSkipLines(1)
            .withType(CreditMutCsvLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
