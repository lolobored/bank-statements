package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.OCBCCSVLine;
import org.lolobored.bankstatements.service.conversion.OCBCCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class OCBCCSVConversionServiceImpl implements OCBCCSVConversionService {

    private static SimpleDateFormat ocbcCSVDate = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public Statement convertCSVToTransactions(String accountNumber, String accountType, String csv) throws ParseException {
        Statement statement = new Statement();
        statement.setAccountNumber(accountNumber);
        statement.setAccountType(accountType);
        statement.setCurrency("SGD");

        List<OCBCCSVLine> ocbccsvLines = parseCSV(csv);

        for (OCBCCSVLine ocbccsvLine : ocbccsvLines) {
            Transaction transaction= new Transaction();
            transaction.setDate(ocbcCSVDate.parse(ocbccsvLine.getValueDate()));
            BigDecimal amount=BigDecimal.ZERO;
            if (!ocbccsvLine.getDebitAmount().isEmpty()){
                amount= BigDecimal.valueOf(Double.parseDouble("-"+ocbccsvLine.getDebitAmount().replace(",","")));
            }
            else{
                amount= BigDecimal.valueOf(Double.parseDouble(ocbccsvLine.getCreditAmount().replace(",","")));
            }
            transaction.setAmount(amount);
            transaction.setLabel(ocbccsvLine.getDescription().replace("\n"," ").replace("\r",""));
            statement.addTransaction(transaction);
        }
        return statement;
    }

    private List<OCBCCSVLine> parseCSV(String csvContent) {
        ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
        ms.setType(OCBCCSVLine.class);

        Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
        CsvToBean cb = new CsvToBeanBuilder(reader)
                .withSkipLines(6)
                .withSeparator(',')
                .withType(OCBCCSVLine.class)
                .withMappingStrategy(ms)
                .build();

        return cb.parse();
    }
}
