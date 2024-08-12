package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.OCBCCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.OCBCService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class OCBCServiceImpl implements OCBCService {

    @Autowired
    private OCBCCSVConversionService ocbccsvConversionService;

    private Logger logger = LoggerFactory.getLogger(OCBCServiceImpl.class);
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
        accessDownloadPage(webDriver, wait);
        downloadTransactions(webDriver, wait, bank);
        String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());

        statements.add(ocbccsvConversionService.convertTableToTransactions(bank.getAccounts().get(0).getAccountId(),
                Statement.DEBIT_ACCOUNT,
                csvContent));
        return statements;
    }

    private void downloadTransactions(WebDriver webDriver, WebDriverWait wait, Bank bank) throws InterruptedException {
        //Store the web element
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("mfeApp")));
        WebElement iframe = webDriver.findElement(By.className("mfeApp"));
        //Switch to the frame
        webDriver.switchTo().frame(iframe);
        wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));
        // scroll to the bottom of the page
        ((JavascriptExecutor) webDriver).executeScript("window.scrollTo(0, document.body.scrollHeight)");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Download')]")));
        WebElement download = webDriver.findElement(By.xpath("//*[contains(text(), 'Download')]"));
        //click and get the option for csv
        download.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'CSV')]")));
        WebElement csv = webDriver.findElement(By.xpath("//*[contains(text(), 'CSV')]"));
        //download in csv
        csv.click();
    }

    private void accessDownloadPage(WebDriver webDriver, WebDriverWait wait) {
        // wait for the page to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("financial-oneview-tab")));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("no-link")));
        WebElement viewAccounts = webDriver.findElement(By.className("no-link"));
        Actions action = new Actions(webDriver);
        //mouse hover
        action.moveToElement(viewAccounts).perform();

        // try to get the link to details of transactions
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("menu-link2")));
        List<WebElement> links = webDriver.findElements(By.className("menu-link2"));
        for (WebElement link : links) {
            if ("Current & savings".equals(link.getText())){
                link.sendKeys(Keys.RETURN);
                break;
            }
        }
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
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("access-code")));
        WebElement loginField = webDriver.findElement(By.id("access-code"));
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("access-pin")));
        WebElement passwordField = webDriver.findElement(By.id("access-pin"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("loginbtn")));
        WebElement loginButton = webDriver.findElement(By.id("loginbtn"));
        loginButton.sendKeys(Keys.RETURN);
    }
}
