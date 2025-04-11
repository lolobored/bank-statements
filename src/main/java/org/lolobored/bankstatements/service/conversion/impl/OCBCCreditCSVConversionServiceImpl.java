package org.lolobored.bankstatements.service.conversion.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.csv.OCBCCreditCSVLine;
import org.lolobored.bankstatements.service.conversion.OCBCCSVConversionService;
import org.lolobored.bankstatements.service.conversion.OCBCCreditCSVConversionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class OCBCCreditCSVConversionServiceImpl implements OCBCCreditCSVConversionService {

    private static SimpleDateFormat ocbcCSVDate = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public Statement convertTableToTransactions(String accountNumber, String accountType, String csv) throws ParseException {
        Statement statement = new Statement();
        statement.setAccountNumber(accountNumber);
        statement.setAccountType(accountType);
        statement.setCurrency("SGD");

        List<OCBCCreditCSVLine> ocbccsvLines = parseCSV(csv);

        for (OCBCCreditCSVLine ocbccsvLine : ocbccsvLines) {
            Transaction transaction= new Transaction();
            transaction.setDate(ocbcCSVDate.parse(ocbccsvLine.getDate()));
            BigDecimal amount=BigDecimal.ZERO;
            if (!ocbccsvLine.getDebitAmount().isEmpty()){
                amount= BigDecimal.valueOf(Double.parseDouble("-"+ocbccsvLine.getDebitAmount().replace(",","")));
            }
            else{
                amount= BigDecimal.valueOf(Double.parseDouble(ocbccsvLine.getCreditAmount().replace(",","")));
            }
            transaction.setAmount(amount);
            transaction.setType(getTransactionType(ocbccsvLine.getDescription(), amount));
            transaction.setLabel(getDescription(ocbccsvLine.getDescription()));
            statement.addTransaction(transaction);
        }
        return statement;
    }

    private List<OCBCCreditCSVLine> parseCSV(String csvContent) {
        ColumnPositionMappingStrategy ms = new ColumnPositionMappingStrategy();
        ms.setType(OCBCCreditCSVLine.class);

        Reader reader = new BufferedReader(new StringReader(csvContent.trim()));
        CsvToBean cb = new CsvToBeanBuilder(reader)
                .withSkipLines(7)
                .withSeparator(',')
                .withType(OCBCCreditCSVLine.class)
                .withMappingStrategy(ms)
                .build();

        return cb.parse();
    }

    private String getDescription(String description){
        if (description.startsWith("FAST PAYMENT")){
            description= StringUtils.substringAfter(description, "\n");
            description= description.replaceAll(" +", " ");
        }
        else if (description.startsWith("DEBIT PURCHASE")){
            description= StringUtils.substringAfter(description, "\n");
            description= StringUtils.substringAfter(description, "xx-");
            description= StringUtils.substringAfter(description, " ");
            description= description.replaceAll(" +", " ");
            description= description.replaceAll("[0-9]{2}/[0-9]{2}/[0-9]{2}$", "");
        }
        else if (description.startsWith("NETS QR")){
            description= StringUtils.substringAfter(description, "\n");
            description= description.replaceAll(" +", " ");
        }
        else if (description.startsWith("BONUS INTEREST")){
            description= StringUtils.substringAfter(description, "\n");
            description= description.replaceAll(" +", " ");
        }
        else if (description.startsWith("FUND TRANSFER")){
            description= StringUtils.substringAfter(description, "\n");
            description= description.replaceAll(" +", " ");
        }
        else if (description.startsWith("PAYMENT/TRANSFER")){
            description= StringUtils.substringAfter(description, "\n");
            description= description.replaceAll(" +", " ");
        }
        else if (description.startsWith("INTEREST CREDIT")){
            description="Interests credit";
        }
        else if (description.startsWith("GIRO - SALARY")){
            description= StringUtils.substringAfter(description, "\n");
            description= description.replaceAll(" +", " ");
        }
        else if (description.startsWith("CASH REBATE")){
            description= StringUtils.substringAfter(description, "\n");
            description= description.replaceAll(" +", " ");
        }

        // remove the S followed by date at the end of the description as it seems to create issues when reconciling
        description= description.replaceAll("[0-9]{2}/[0-9]{2}/[0-9]{2}", "");
        description= description.replaceAll("\\\\s+[$\\n]", "");
        description= description.trim();
        return description;
    }

    private String getTransactionType(String description, BigDecimal amount){
        if (description.startsWith("FAST PAYMENT")){
            return Transaction.XFER_TYPE;
        }
        else if (description.startsWith("DEBIT PURCHASE")){
            return Transaction.DEBIT_TYPE;
        }
        else if (description.startsWith("NETS QR")){
            return Transaction.DEBIT_TYPE;
        }
        else if (description.startsWith("BONUS INTEREST")){
            return Transaction.CREDIT_TYPE;
        }
        else if (description.startsWith("FUND TRANSFER")){
            return Transaction.DEBIT_TYPE;
        }
        else if (description.startsWith("PAYMENT/TRANSFER")){
            return Transaction.XFER_TYPE;
        }
        else if (description.startsWith("INTEREST CREDIT")){
            return Transaction.CREDIT_TYPE;
        }
        else if (description.startsWith("GIRO - SALARY")){
            return Transaction.CREDIT_TYPE;
        }
        else if (amount.compareTo(BigDecimal.ZERO) >0){
            return Transaction.CREDIT_TYPE;
        }
        return Transaction.DEBIT_TYPE;
    }
}
