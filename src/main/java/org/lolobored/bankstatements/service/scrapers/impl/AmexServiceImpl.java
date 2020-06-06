package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.service.conversion.AmexCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.AmexService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmexServiceImpl implements AmexService {

    private Logger logger = LoggerFactory.getLogger(AmexServiceImpl.class);

    @Autowired
    private AmexCSVConversionService amexCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {
        List<Statement> statements = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(webDriver, bank.getWaitTime());

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
         * There might be a cookie confirmation
         * panel preventing to click on the buttons
         */
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("sprite-AcceptButton_EN")));
            WebElement cookiesButton = webDriver.findElement(By.id("sprite-AcceptButton_EN"));
            cookiesButton.click();
        } catch (Exception err) {
            logger.warn("No cookies window found");
        }

        /**
         * Look for the username
         * and password
         */
        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("eliloUserID"))));
        WebElement loginField = webDriver.findElement(By.id("eliloUserID"));
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("eliloPassword"))));
        WebElement passwordField = webDriver.findElement(By.id("eliloPassword"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("loginSubmit")));
        WebElement loginButton = webDriver.findElement(By.id("loginSubmit"));
        loginButton.submit();


        wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-block")));

        /**
         * Now let's go to the statements page
         */

        webDriver.navigate().to("https://global.americanexpress.com/myca/intl/download/emea/download.do?request_type=&Face=en_GB&BPIndex=0&sorted_index=0&inav=gb_myca_pc_statement_export_statement_data");
        wait = new WebDriverWait(webDriver, bank.getWaitTime());

        /**
         * There might be a cookie confirmation remaining
         * panel preventing to click on the buttons
         */
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("AcceptButton_EN")));
            WebElement cookiesButton = webDriver.findElement(By.id("AcceptButton_EN"));
            cookiesButton.click();
        } catch (Exception err) {
            logger.warn("No cookies window found");
        }

        /**
         * Select a CSV type of download
         */
        WebElement csvRadio = webDriver.findElement(By.id("CSV"));
        csvRadio.click();

        /**
         * Select the accounts
         */
        List<WebElement> cardsDiv = webDriver.findElements(By.id("outer_inside"));

        List<WebElement> accountRadios = cardsDiv.get(1).findElements(By.className("selectCardRadio"));
        List<WebElement> accountNames = cardsDiv.get(1).findElements(By.className("imagedetails"));

        for (int i=0; i< accountRadios.size(); i++) {
            WebElement accountRadio = accountRadios.get(i);
            if (!accountRadio.isDisplayed()){
                continue;
            }
            /**
             * Select the account
             */
            accountRadio.click();

            String imagedetails = accountNames.get(i).getText().trim();
            String accountName= StringUtils.substringAfterLast(imagedetails, "\n");
            /**
             * Now there might be multiple period
             * we can download
             * Ideally we need to select everything possible.
             * The buttons are named radioid+ sequential
             * Iterate through those.
             */
            int periodId = 0;
            try {
                while (true) {
                    String period = StringUtils.leftPad(String.valueOf(periodId), 2, "0");
                    WebElement periodButton = webDriver.findElement(By.id("radioid" + period));
                    periodId++;
                    if (!periodButton.isSelected()) {
                        periodButton.click();
                    }
                }
            } catch (Exception err) {
                logger.info("Last button hit. Total periods taken into account: " + (periodId));
            }

            /**
             * finally download the csv file
             */
            WebElement downloadButton = webDriver.findElement(By.id("myBlueButton1"));
            wait.until(ExpectedConditions.elementToBeClickable(downloadButton));
            Actions actions = new Actions(webDriver);
            actions.moveToElement(downloadButton).click().perform();

            String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
            statements.add(amexCSVConversionService.convertCSVToTransactions(accountName, Statement.CREDIT_CARD, csvContent));
        }
        return statements;
    }
}
