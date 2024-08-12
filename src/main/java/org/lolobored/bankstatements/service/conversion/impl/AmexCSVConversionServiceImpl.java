package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.AmexCsvLine;
import org.lolobored.bankstatements.service.conversion.AmexCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class AmexCSVConversionServiceImpl implements AmexCSVConversionService {

  private static SimpleDateFormat amexCSVDate = new SimpleDateFormat("dd/MM/yy");

  @Override
  public Statement convertTableToTransactions(String accountNumber, String accountType, String csv) throws ParseException {

    Statement statement = new Statement();
    statement.setAccountNumber(accountNumber);
    statement.setAccountType(accountType);
    statement.setCurrency("GBP");

    List<AmexCsvLine> amexCSVLines = parseCSV(csv);
    for (AmexCsvLine amexCSVLine : amexCSVLines) {
      Transaction transaction = new Transaction();
      transaction.setReference(amexCSVLine.getReference().trim().replace("'", ""));
      transaction.setLabel(amexCSVLine.getDescription());
      transaction.setDate(amexCSVDate.parse(amexCSVLine.getDate()));
      // credit
      if (amexCSVLine.getAmount().trim().startsWith("-")) {
        transaction.setAmount(new BigDecimal(amexCSVLine.getAmount().trim().substring(1)));
      } else {
        transaction.setAmount(new BigDecimal("-" + amexCSVLine.getAmount().trim()));
      }


      transaction.setAdditionalInformation(amexCSVLine.getExtendedDetails());
      statement.addTransaction(transaction);
    }
    return statement;
  }

  private List<AmexCsvLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
    ms.setType(AmexCsvLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean cb = new CsvToBeanBuilder(reader)
            .withSkipLines(1)
            .withSeparator(',')
            .withType(AmexCsvLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
