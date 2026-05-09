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
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.AmexCsvLine;
import org.lolobored.bankstatements.service.conversion.AmexCSVConversionService;
import org.springframework.stereotype.Service;

@Service
public class AmexCSVConversionServiceImpl implements AmexCSVConversionService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

  @Override
  public Statement convertTableToTransactions(String accountNumber, String accountType, String csv)
      throws ParseException {

    Statement statement = new Statement();
    statement.setAccountNumber(accountNumber);
    statement.setAccountType(accountType);
    statement.setCurrency("GBP");

    List<AmexCsvLine> amexCSVLines = parseCSV(csv);
    for (AmexCsvLine amexCSVLine : amexCSVLines) {
      Transaction transaction = new Transaction();
      transaction.setReference(amexCSVLine.getReference().trim().replace("'", ""));
      transaction.setLabel(amexCSVLine.getDescription());
      transaction.setDate(LocalDate.parse(amexCSVLine.getDate(), DATE_FORMATTER));
      // credit
      if (amexCSVLine.getAmount().trim().startsWith("-")) {
        transaction.setAmount(new BigDecimal(amexCSVLine.getAmount().trim().substring(1)));
        transaction.setType(Transaction.CREDIT_TYPE);
      } else {
        transaction.setAmount(new BigDecimal("-" + amexCSVLine.getAmount().trim()));
        transaction.setType(Transaction.DEBIT_TYPE);
      }

      transaction.setAdditionalInformation(amexCSVLine.getExtendedDetails());
      statement.addTransaction(transaction);
    }
    return statement;
  }

  private List<AmexCsvLine> parseCSV(String csvContent) {
    ColumnPositionMappingStrategy<AmexCsvLine> ms = new ColumnPositionMappingStrategy<>();
    ms.setType(AmexCsvLine.class);

    Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
    CsvToBean<AmexCsvLine> cb =
        new CsvToBeanBuilder<AmexCsvLine>(reader)
            .withSkipLines(1)
            .withSeparator(',')
            .withType(AmexCsvLine.class)
            .withMappingStrategy(ms)
            .build();

    return cb.parse();
  }
}
