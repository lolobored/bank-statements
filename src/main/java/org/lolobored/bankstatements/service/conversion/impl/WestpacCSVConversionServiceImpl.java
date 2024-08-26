package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.WestpacCSVLine;
import org.lolobored.bankstatements.service.conversion.WestpacCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class WestpacCSVConversionServiceImpl implements WestpacCSVConversionService {

    private static SimpleDateFormat westpacCSVDate = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public Statement convertTableToTransactions(String accountNumber, String accountType, String csv) throws ParseException {
        Statement statement = new Statement();
        statement.setAccountNumber(accountNumber);
        statement.setAccountType(accountType);
        statement.setCurrency("AUD");

        List<WestpacCSVLine> westpacCSVLines = parseCSV(csv);

        for (WestpacCSVLine westpacCSVLine : westpacCSVLines) {
            Transaction transaction= new Transaction();
            transaction.setDate(westpacCSVDate.parse(westpacCSVLine.getDate()));
            BigDecimal amount=BigDecimal.ZERO;
            if (westpacCSVLine.getCreditAmount().isEmpty()){
                amount= BigDecimal.valueOf(Double.parseDouble("-"+westpacCSVLine.getDebitAmount().replace(",","")));
                transaction.setType(Transaction.DEBIT_TYPE);
            }
            else{
                amount= BigDecimal.valueOf(Double.parseDouble(westpacCSVLine.getCreditAmount().replace(",","")));
                transaction.setType(Transaction.CREDIT_TYPE);
            }
            transaction.setAmount(amount);
            transaction.setLabel(westpacCSVLine.getNarrative().replace("\n"," ").replace("\r",""));
            statement.addTransaction(transaction);
        }
        return statement;
    }

    private List<WestpacCSVLine> parseCSV(String csvContent) {
        ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
        ms.setType(WestpacCSVLine.class);

        Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
        CsvToBean cb = new CsvToBeanBuilder(reader)
                .withSkipLines(1)
                .withSeparator(',')
                .withType(WestpacCSVLine.class)
                .withMappingStrategy(ms)
                .build();

        return cb.parse();
    }
}
