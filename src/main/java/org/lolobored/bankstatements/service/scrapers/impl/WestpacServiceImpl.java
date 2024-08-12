package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.WestpacCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.WestpacService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.*;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class WestpacServiceImpl implements WestpacService {
    private Logger logger = LoggerFactory.getLogger(WestpacServiceImpl.class);

    @Autowired
    private WestpacCSVConversionService westpacCSVConversionService;

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
        goToDownloadPage(webDriver);
        fillDownloadPage(webDriver, wait);

        String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
        statements.add(westpacCSVConversionService.convertTableToTransactions(bank.getAccounts().get(0).getAccountId(),
                Statement.CREDIT_CARD, csvContent));
        return statements;
    }

    private void fillDownloadPage(WebDriver webDriver, WebDriverWait wait) {
        DateTimeFormatter dateFormat= DateTimeFormatter.ofPattern("dd/MM/yyyy");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("DateRange_StartDate")));
        WebElement dateFromField = webDriver.findElement(By.id("DateRange_StartDate"));
        dateFromField.clear();
        dateFromField.sendKeys(LocalDate.now().minusMonths(1).format(dateFormat));

        WebElement selectMultipleButton= webDriver.findElement(By.className("select-multiple"));
        selectMultipleButton.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("_selectall")));
        WebElement selectAll = webDriver.findElement(By.id("_selectall"));
        selectAll.click();

        WebElement submitAccounts = webDriver.findElement(By.className("btn-submit"));
        submitAccounts.click();

        // scroll to the bottom of the page
        ((JavascriptExecutor) webDriver).executeScript("window.scrollTo(0, document.body.scrollHeight)");

        WebElement download = webDriver.findElement(By.className("export-link"));
        download.click();

    }

    private void goToDownloadPage(WebDriver webDriver) {
        webDriver.get("https://banking.westpac.com.au/secure/banking/reportsandexports/exportparameters/2/");
    }

    private void executeLogin(WebDriver webDriver, WebDriverWait wait, Bank bank){
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
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fakeusername")));
        WebElement loginField = webDriver.findElement(By.id("fakeusername"));
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        WebElement passwordField = webDriver.findElement(By.id("password"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("signin")));
        WebElement loginButton = webDriver.findElement(By.id("signin"));
        loginButton.sendKeys(Keys.RETURN);
    }
}
