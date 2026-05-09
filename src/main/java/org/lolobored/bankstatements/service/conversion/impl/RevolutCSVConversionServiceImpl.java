package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.RevolutCSVLine;
import org.lolobored.bankstatements.service.conversion.RevolutCSVConversionService;
import org.springframework.stereotype.Service;

@Service
public class RevolutCSVConversionServiceImpl implements RevolutCSVConversionService {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

  @Override
  public Statement convertTableToTransactions(String accountNumber, String accountType, String csv)
      throws ParseException {
    String thisYear = String.valueOf(LocalDate.now().getYear());
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
        tx.setDate(LocalDate.parse(csvLine.getDate() + " " + thisYear, DATE_FORMATTER));
      } else {
        tx.setDate(LocalDate.parse(csvLine.getDate(), DATE_FORMATTER));
      }

      if (StringUtils.isNotEmpty(csvLine.getAmountIn())) {
        tx.setAmount(new BigDecimal(csvLine.getAmountIn().replace(",", "")));
        tx.setType(Transaction.CREDIT_TYPE);

      } else {
        tx.setAmount(new BigDecimal("-" + csvLine.getAmountOut().replace(",", "")));
        tx.setType(Transaction.DEBIT_TYPE);
      }
      tx.setLabel(csvLine.getReference());
      tx.setAdditionalInformation(csvLine.getCategory());
      statement.addTransaction(tx);
    }
    return statement;
  }

  private List<RevolutCSVLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy<RevolutCSVLine> ms = new ColumnPositionMappingStrategy<>();
    ms.setType(RevolutCSVLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean<RevolutCSVLine> cb =
        new CsvToBeanBuilder<RevolutCSVLine>(reader)
            .withSeparator(';')
            .withSkipLines(1)
            .withType(RevolutCSVLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
