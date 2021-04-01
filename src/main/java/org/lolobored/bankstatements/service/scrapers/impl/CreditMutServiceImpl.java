package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.service.conversion.CreditMutCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.CreditMutService;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class CreditMutServiceImpl implements CreditMutService {

    private Logger logger = LoggerFactory.getLogger(CreditMutServiceImpl.class);

    @Autowired
    private CreditMutCSVConversionService creditMutCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws IOException, InterruptedException, ParseException {
        WebDriverWait wait = new WebDriverWait(webDriver, bank.getWaitTime());
        List<Statement> statements = new ArrayList<>();

        /**
         * Delete the download directory
         */
        File downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

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
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("_userid")));
        WebElement loginField = webDriver.findElement(By.id("_userid"));
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("_pwduser")));
        WebElement passwordField = webDriver.findElement(By.id("_pwduser"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#login-submit > a")));
        WebElement loginButton = webDriver.findElement(By.cssSelector("#login-submit > a"));
        loginButton.sendKeys(Keys.RETURN);

        /**
         * Validate the cookies
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"cookieLBmainbuttons\"]/span/span[1]/span/a")));
        WebElement cookieValidation = webDriver.findElement(By.xpath("//*[@id=\"cookieLBmainbuttons\"]/span/span[1]/span/a"));
        cookieValidation.click();
        /**
         * Now let's go to the download page as soon as we can see the download button (after confirming by  mobile)
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("I0:s2.A3:C2:triggerLink")));
        webDriver.navigate().to("https://www.creditmutuel.fr/fr/banque/compte/telechargement.cgi");
        /**
         * Select a CSV type of download
         */
        WebElement csvRadio = webDriver.findElement(By.id("csv:DataEntry"));
        csvRadio.click();
        /**
         * Now select the accounts one after the other and download
         */
        int currentElementId = 0;
        WebElement previousElement = null;
        while (true) {
            String id = "F_" + currentElementId + ".accountCheckbox:DataEntry";
            WebElement accountCheck = null;
            try {
                accountCheck = webDriver.findElement(By.id(id));
            } catch (NoSuchElementException none) {
                logger.info("Last element downloaded... Continuing");
                break;
            }

            if (previousElement != null) {
                previousElement.click();
            }
            accountCheck.click();
            previousElement = accountCheck;
            WebElement downloadButton = webDriver.findElement(By.id("B3"));
            wait.until(ExpectedConditions.elementToBeClickable(downloadButton));
            Actions actions = new Actions(webDriver);
            actions.moveToElement(downloadButton).click().perform();

            String accountNumber= FileUtility.getDownloadedFilename(downloads, bank.getWaitTime());
            accountNumber= accountNumber.replace(".csv", "");
            String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
            statements.add(creditMutCSVConversionService.convertCSVToTransactions(accountNumber, Statement.DEBIT_ACCOUNT, csvContent));

            currentElementId++;
        }
        return statements;
    }
}
