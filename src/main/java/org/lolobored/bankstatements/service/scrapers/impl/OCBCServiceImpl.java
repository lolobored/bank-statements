package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.OCBCCSVConversionService;
import org.lolobored.bankstatements.service.conversion.OCBCCreditCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.OCBCService;
import org.lolobored.bankstatements.utils.AccountUtils;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class OCBCServiceImpl implements OCBCService {

    @Autowired
    private OCBCCSVConversionService ocbccsvConversionService;
    @Autowired
    private OCBCCreditCSVConversionService ocbcCreditCSVConversionService;

    private final static int WAIT_TIME= 3000;

    private Logger logger = LoggerFactory.getLogger(OCBCServiceImpl.class);
    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws Exception {
        List<Statement> statements = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));
        /**
         * Delete the download directory
         */
        File downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        executeLogin(webDriver, wait, bank);

        for (Account account : bank.getAccounts()) {
            if (Account.DEBIT.equals(account.getType())){
                // download credits statement
                accessAccountDetails(webDriver, wait, "mfe-cfo-portfolio--MuiPaper-root", account.getAccountName(), bank);
                downloadTransactions(webDriver, wait, bank);
                String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());

                statements.add(ocbccsvConversionService.convertTableToTransactions(account.getAccountId(),
                        Statement.DEBIT_ACCOUNT,
                        csvContent));
            }
        }



        downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        for (Account account : bank.getAccounts()) {
            if (Account.CREDIT.equals(account.getType())) {
                // scroll back to top
                //webDriver.switchTo().defaultContent();
                WebElement logo = webDriver.findElement(By.id("content"));
                ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", logo);

                // come back to overview
                accessDownloadPage(webDriver, wait, "Overview", bank);
                // download credits statement
                accessAccountDetails(webDriver, wait, "wyoMainContainer", account.getAccountName(), bank);
                downloadCreditsTransactions(webDriver, wait, bank);
                String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());

                statements.add(ocbcCreditCSVConversionService.convertTableToTransactions(account.getAccountId(),
                        Statement.CREDIT_CARD,
                        csvContent));
            }
        }


        return statements;
    }

    private void downloadTransactions(WebDriver webDriver, WebDriverWait wait, Bank bank) throws InterruptedException, IOException {
        //Store the web element
        Thread.sleep(WAIT_TIME);

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

    private void downloadCreditsTransactions(WebDriver webDriver, WebDriverWait wait, Bank bank) throws InterruptedException, IOException {
        Thread.sleep(WAIT_TIME);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dropdown-cards-list")));
        WebElement frequency = webDriver.findElement(By.id("dropdown-cards-list"));
        ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", frequency);
        Thread.sleep(WAIT_TIME);
        frequency.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Past 30 Days')]")));
        frequency = webDriver.findElement(By.xpath("//*[contains(text(), 'Past 30 Days')]"));
        frequency.click();
        Thread.sleep(WAIT_TIME);

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

    private void accessAccountDetails(WebDriver webDriver, WebDriverWait wait, String blockClassName, String accountName, Bank bank) throws Exception {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(blockClassName)));
        WebElement owingBlock = webDriver.findElement(By.className(blockClassName));

        ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", owingBlock);
        Thread.sleep(WAIT_TIME);
        List<WebElement> accountClickableBlocks = owingBlock.findElements(By.className("cursor"));
        for (WebElement accountClickableBlock : accountClickableBlocks) {
            if (accountName.equals(AccountUtils.getCleanedAccount(accountClickableBlock.getText()))) {
                Actions action = new Actions(webDriver);
               // ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", accountClickableBlock);
                action.moveToElement(accountClickableBlock).pause(Duration.ofMillis(WAIT_TIME)).build().perform();
                // find the clickable "Details Transaction"
                List<WebElement> linksBlocks = webDriver.findElements(By.className("first"));
                System.out.print("size"+linksBlocks.size());
                for (WebElement linksBlock : linksBlocks) {
                    if ("Details / Transactions".equals(linksBlock.getText())) {
                        linksBlock.click();
                        return;
                    }
                }
                throw new IOException("Couldn't find account id "+accountName);
            }
        }
    }

    private void accessDownloadPage(WebDriver webDriver, WebDriverWait wait, String linkName, Bank bank) throws InterruptedException {
        // wait for the page to appear
        //wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mfe-cfo-portfolio--MuiAccordionSummary-content")));
        Thread.sleep(WAIT_TIME);
        //     wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("banner--menu-text-en")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'View accounts')]")));
        WebElement viewAccount = webDriver.findElement(By.xpath("//*[contains(text(), 'View accounts')]"));
        Actions action = new Actions(webDriver);
        //mouse hover
        action.moveToElement(viewAccount).pause(Duration.ofMillis(WAIT_TIME)).build().perform();
        Thread.sleep(WAIT_TIME);
        // try to get the link to details of transactions
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), '"+linkName+"')]")));
        WebElement link = webDriver.findElement(By.xpath("//*[contains(text(), '"+linkName+"')]"));
        link.click();
        //link.sendKeys(Keys.RETURN);

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
