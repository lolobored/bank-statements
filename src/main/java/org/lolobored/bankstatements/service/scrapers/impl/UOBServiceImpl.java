package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.UOBXLSConversionService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class UOBServiceImpl implements UOBService {
    private Logger logger = LoggerFactory.getLogger(UOBServiceImpl.class);

    private static int WAIT_TIME=3000;

    @Autowired
    private UOBXLSConversionService uobxlsConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {
        List<Statement> statements = new ArrayList<>();

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));
        /**
         * Delete the download directory
         */
        File downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        executeLogin(webDriver, wait, bank);
        return accessDownloadPage(webDriver, wait, bank, downloads);

    }

    private List<Statement> accessDownloadPage(WebDriver webDriver, WebDriverWait wait, Bank bank, File downloads) throws IOException, InterruptedException, ParseException {
        List<Statement> statements = new ArrayList<>();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("color-account")));
        List<WebElement> accounts = webDriver.findElements(By.className("color-account"));
        for (Account bankAccount : bank.getAccounts()) {
            boolean found = false;
            for (WebElement account : accounts) {

                if (StringUtils.equalsIgnoreCase(bankAccount.getAccountId().trim(), account.getText().trim())) {
                    Thread.sleep(WAIT_TIME);
                    account.click();
                    statements.addAll(downloadTransactions(webDriver, wait, bank.getWaitTime(), bankAccount, downloads));
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

    private List<Statement> downloadTransactions(WebDriver webDriver, WebDriverWait wait, int waitTime, Account bankAccount, File downloads) throws IOException, InterruptedException, ParseException {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Statement> statements = new ArrayList<>();
        Statement currentStatement, previousStatement;

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("frequency-account-summary")));
        WebElement frequency = webDriver.findElement(By.id("frequency-account-summary"));
        frequency.sendKeys("Current Month");
        Thread.sleep(WAIT_TIME);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("i-download2")));
        WebElement download = webDriver.findElement(By.className("i-download2"));
        download.click();

        String xlsFile = FileUtility.getDownloadedFilename(downloads, waitTime);
        currentStatement= uobxlsConversionService.convertTableToTransactions(bankAccount.getAccountId(), Statement.DEBIT_ACCOUNT, downloads.getAbsolutePath()+"/"+xlsFile);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        frequency = webDriver.findElement(By.id("frequency-account-summary"));
        frequency.sendKeys("Previous Month");
        Thread.sleep(WAIT_TIME);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("i-download2")));
        download = webDriver.findElement(By.className("i-download2"));
        download.click();

        xlsFile = FileUtility.getDownloadedFilename(downloads, waitTime);
        previousStatement=uobxlsConversionService.convertTableToTransactions(bankAccount.getAccountId(), Statement.DEBIT_ACCOUNT, downloads.getAbsolutePath()+"/"+xlsFile);
        currentStatement.getTransactions().addAll(previousStatement.getTransactions());
        statements.add(currentStatement);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();
        return statements;

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
}
