package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.scrapers.UOBService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class UOBServiceImpl implements UOBService {
    private Logger logger = LoggerFactory.getLogger(UOBServiceImpl.class);

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {
        List<Statement> statements = new ArrayList<>();

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));
        WebDriverWait waitSMS = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitSMSTime()));
        /**
         * Delete the download directory
         */
        File downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        executeLogin(webDriver, wait, bank);
        return accessDownloadPage(webDriver, wait, waitSMS, bank, downloads);

    }

    private List<Statement> accessDownloadPage(WebDriver webDriver, WebDriverWait wait, WebDriverWait waitSMS, Bank bank, File downloads) throws IOException, InterruptedException, ParseException {
        List<Statement> statements = new ArrayList<>();
        waitSMS.until(ExpectedConditions.visibilityOfElementLocated(By.className("color-account")));
        List<WebElement> accounts = webDriver.findElements(By.className("color-account"));
        for (Account bankAccount : bank.getAccounts()) {
            boolean found = false;
            for (WebElement account : accounts) {

                if (StringUtils.equalsIgnoreCase(bankAccount.getAccountId().trim(), account.getText().trim())) {
                    account.click();
                    List<Transaction> transactions = downloadTransactions(webDriver, wait, waitSMS, bank, downloads);
                    Statement statement = new Statement();
                    statement.setAccountNumber(bankAccount.getAccountId());
                    statement.setAccountType(Statement.DEBIT_ACCOUNT);
                    statement.setCurrency("SGD");
                    statement.setTransactions(transactions);
                    statements.add(statement);
                    // going back
                    webDriver.findElement(By.className("uob-dashboard")).click();
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IOException("Unable to find account [" + bankAccount.getAccountId() + "] on the page");
            }
        }
        return statements;
    }

    private List<Transaction> downloadTransactions(WebDriver webDriver, WebDriverWait wait, WebDriverWait waitSMS, Bank bank, File downloads) throws IOException, InterruptedException, ParseException {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Transaction> transactions = new ArrayList<>();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("frequency-account-summary")));
        WebElement frequency = webDriver.findElement(By.id("frequency-account-summary"));
        frequency.sendKeys("Current Month");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("i-download2")));
        WebElement download = webDriver.findElement(By.className("i-download2"));
        download.click();

        String xlsFile = FileUtility.getDownloadedFilename(downloads, bank.getWaitTime());
        transactions = getTransactions(downloads.getAbsolutePath() + "/" + xlsFile);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        frequency = webDriver.findElement(By.id("frequency-account-summary"));
        frequency.sendKeys("Previous Month");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("i-download2")));
        download = webDriver.findElement(By.className("i-download2"));
        download.click();

        xlsFile = FileUtility.getDownloadedFilename(downloads, bank.getWaitTime());
        transactions.addAll(getTransactions(downloads.getAbsolutePath() + "/" + xlsFile));
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();
        return transactions;

    }

    private void executeLogin(WebDriver webDriver, WebDriverWait wait, Bank bank) {
        /**
         * Login to the main page
         */
        logger.info("Connecting to " + bank.getConnectionUrl());

        webDriver.get(bank.getConnectionUrl());

        logger.info("Connected to " + bank.getConnectionUrl());

        /**
         * Look for the username
         * and password
         */
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("userName")));
        WebElement loginField = webDriver.findElement(By.id("userName"));
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("PASSWORD1")));
        WebElement passwordField = webDriver.findElement(By.id("PASSWORD1"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btnSubmit")));
        WebElement loginButton = webDriver.findElement(By.id("btnSubmit"));
        loginButton.sendKeys(Keys.RETURN);
    }

    private List<Transaction> getTransactions(String xlsPath) throws IOException, ParseException {
        HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(xlsPath));
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
                } else {
                    tx.setAmount(BigDecimal.valueOf(row.getCell(3).getNumericCellValue()));
                }
                transactions.add(tx);
            }
        }
        return transactions;
    }
}
