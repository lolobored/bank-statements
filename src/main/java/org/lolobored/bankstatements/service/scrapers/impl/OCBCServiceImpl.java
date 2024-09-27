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

import java.awt.*;
import java.awt.event.KeyEvent;
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

    private final static int WAIT_TIME= 3000;

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

    private void downloadTransactions(WebDriver webDriver, WebDriverWait wait, Bank bank) throws InterruptedException, IOException {
        //Store the web element
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("mfeApp")));
        //Thread.sleep(20000);
        //Switch to the frame
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.className("mfeApp")));
      //  WebElement iframe = webDriver.findElement(By.className("mfeApp"));
      //  webDriver.switchTo().frame(iframe);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Download')]")));
        WebElement download = webDriver.findElement(By.xpath("//*[contains(text(), 'Download')]"));
        ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", download);
        Thread.sleep(WAIT_TIME);
        //click and get the option for csv
        download.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'CSV')]")));
        WebElement csv = webDriver.findElement(By.xpath("//*[contains(text(), 'CSV')]"));
        //download in csv
        csv.click();
    }

    private void accessDownloadPage(WebDriver webDriver, WebDriverWait wait) throws InterruptedException {
        // wait for the page to appear
        //wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mfe-cfo-portfolio--MuiAccordionSummary-content")));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("banner--menu-text-en")));
        List<WebElement> viewAccounts = webDriver.findElements(By.className("banner--menu-text-en"));
        for (WebElement viewAccount : viewAccounts) {
            if ("View accounts".equalsIgnoreCase(viewAccount.getText())){
                Actions action = new Actions(webDriver);
                //mouse hover
                action.moveToElement(viewAccount).pause(Duration.ofSeconds(2)).build().perform();
                break;
            }
        }

        Thread.sleep(WAIT_TIME);

        // try to get the link to details of transactions
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("MuiTypography-root")));
        List<WebElement> links = webDriver.findElements(By.className("MuiTypography-root"));
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
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("textAccessCode")));
        WebElement loginField = webDriver.findElement(By.id("textAccessCode"));
        Actions actions = new Actions(webDriver);
        actions.moveToElement(loginField).pause(Duration.ofSeconds(2)).build().perform();
        // give it a bit of time before inputting the text (there's an animation)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("textLoginPin")));
        WebElement passwordField = webDriver.findElement(By.id("textLoginPin"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("cmdLogin")));
        WebElement loginButton = webDriver.findElement(By.id("cmdLogin"));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        loginButton.click();
    }
}
