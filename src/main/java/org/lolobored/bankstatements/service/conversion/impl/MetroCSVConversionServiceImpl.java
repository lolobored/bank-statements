package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.MetroCSVLine;
import org.lolobored.bankstatements.service.conversion.MetroCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class MetroCSVConversionServiceImpl implements MetroCSVConversionService {

  private static SimpleDateFormat metroCSVDate = new SimpleDateFormat("dd/MM/yyyy");

  @Override
  public Statement convertTableToTransactions(String accountNumber, String accountType, String csv) throws ParseException {
    Statement statement = new Statement();
    statement.setAccountNumber(accountNumber);
    statement.setAccountType(accountType);
    statement.setCurrency("GBP");

    List<MetroCSVLine> metroCSVLines = parseCSV(csv);
    for (MetroCSVLine metroCSVLine : metroCSVLines) {
      Transaction transaction = new Transaction();
      transaction.setLabel(metroCSVLine.getLabel());
      transaction.setDate(metroCSVDate.parse(metroCSVLine.getDate()));
      if (StringUtils.isEmpty(metroCSVLine.getMoneyIn()) || metroCSVLine.getMoneyIn().equals("0.00")) {
        transaction.setAmount(new BigDecimal("-" + metroCSVLine.getMoneyOut()));
      } else {
        transaction.setAmount(new BigDecimal(metroCSVLine.getMoneyIn()));
      }
      statement.addTransaction(transaction);
    }


    return statement;
  }

  private List<MetroCSVLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
    ms.setType(MetroCSVLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean cb = new CsvToBeanBuilder(reader)
            .withSeparator(',')
            .withSkipLines(1)
            .withType(MetroCSVLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
