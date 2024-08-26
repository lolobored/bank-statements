package org.lolobored.bankstatements.service.conversion.impl;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.UOBXLSConversionService;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class UOBXLSConversionServiceImpl implements UOBXLSConversionService{
    @Override
    public Statement convertTableToTransactions(String accountNumber, String accountType, String path) throws ParseException, IOException {
        Statement statement = new Statement();
        statement.setAccountNumber(accountNumber);
        statement.setAccountType(accountType);
        statement.setCurrency("SGD");

        HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(path));
        List<Transaction> transactions = new ArrayList<>();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy");
        boolean transactionStarted = false;
        HSSFSheet sheet = workbook.getSheetAt(0);
        // browse up until row named "Transaction Date"
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (!transactionStarted) {
                if ("Transaction Date".equals(row.getCell(0).getStringCellValue())) {
                    transactionStarted = true;
                }
            } else {
                Transaction tx = new Transaction();
                String dateString = row.getCell(0).getStringCellValue();
                tx.setDate(dateFormatter.parse(dateString));
                tx.setLabel(row.getCell(1).getStringCellValue());
                if (row.getCell(2).getNumericCellValue() > 0) {
                    tx.setAmount(BigDecimal.valueOf(-row.getCell(2).getNumericCellValue()));
                    tx.setType(Transaction.DEBIT_TYPE);
                } else {
                    tx.setAmount(BigDecimal.valueOf(row.getCell(3).getNumericCellValue()));
                    tx.setType(Transaction.CREDIT_TYPE);
                }
                transactions.add(tx);
            }
        }
        statement.setTransactions(transactions);
        return statement;
    }
}
