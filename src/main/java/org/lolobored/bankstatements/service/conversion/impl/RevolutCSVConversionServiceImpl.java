package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.RevolutCSVLine;
import org.lolobored.bankstatements.service.conversion.RevolutCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class RevolutCSVConversionServiceImpl implements RevolutCSVConversionService {

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMMMMMMMMM yyyy");
  private static SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

  @Override
  public Statement convertCSVToTransactions(String accountNumber, String accountType, String csv) throws ParseException {
    String thisYear = yearFormat.format(new Date());
    List<RevolutCSVLine> csvLines = parseCSV(csv);
    String header = StringUtils.substringBefore(csv, "\n");
    String currency = StringUtils.substringBetween(header, "Paid Out (", ")");
    Statement statement = new Statement();
    statement.setAccountType(accountType);
    statement.setAccountNumber(accountNumber);
    statement.setCurrency(currency);
    for (RevolutCSVLine csvLine : csvLines) {
      Transaction tx = new Transaction();
      if (StringUtils.countMatches(csvLine.getDate(), ' ') == 1) {
        tx.setDate(dateFormat.parse(csvLine.getDate() + " " + thisYear));
      } else {
        tx.setDate(dateFormat.parse(csvLine.getDate()));
      }

      if (StringUtils.isNotEmpty(csvLine.getAmountIn())) {
        tx.setAmount(new BigDecimal(csvLine.getAmountIn().replace(",", "")));

      } else {
        tx.setAmount(new BigDecimal("-" + csvLine.getAmountOut().replace(",", "")));
      }
      tx.setLabel(csvLine.getReference());
      tx.setAdditionalInformation(csvLine.getCategory());
      statement.addTransaction(tx);
    }
    return statement;
  }

  private List<RevolutCSVLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
    ms.setType(RevolutCSVLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean cb = new CsvToBeanBuilder(reader)
            .withSeparator(';')
            .withSkipLines(1)
            .withType(RevolutCSVLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
