package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.MetroCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.MetroService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
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
public class MetroServiceImpl implements MetroService {

    private Logger logger = LoggerFactory.getLogger(MetroServiceImpl.class);

    @Autowired
    private MetroCSVConversionService metroCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {

        WebDriverWait wait = new WebDriverWait(webDriver, bank.getWaitTime());
        /**
         * Delete the download directory
         */
        File downloads= new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        /**
         * First go through the login page
         */
        webDriver.get(bank.getConnectionUrl());
        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("USER_NAME"))));
        WebElement loginField = webDriver.findElement(By.id("USER_NAME"));
        loginField.sendKeys(bank.getUsername());

        /**
         * There might be a cookie confirmation
         * panel preventing to click on the buttons
         */
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("js-mbCookieNotice-button")));
            WebElement cookiesButton = webDriver.findElement(By.id("js-mbCookieNotice-button"));
            cookiesButton.click();
        }
        catch (Exception err){
            logger.warn("No cookies window found");
        }

        // find button and click
        wait.until(ExpectedConditions.elementToBeClickable(By.className("ibid-btn")));
        WebElement loginButton = webDriver.findElement(By.className("ibid-btn"));
        loginButton.click();

        /**
         * Let's wait for the second page to come in
         * This is where we have to fill in the password
         * and the security pin
         */

        /**
         * Retrieve and sets the security pin selectors
         */
        for (int i=1; i<4; i++){
            setSecurityPinEntries(webDriver, wait, i, bank.getSecurityPin());
        }
        /**
         * Retrieve and sets the password text
         */
        for (int i=1; i<4; i++){
            setPasswordEntries(webDriver, wait, i, bank.getPassword());
        }
        //Thread.sleep(1000* bank.getWaitTime());
        // validate the form
        wait.until(ExpectedConditions.elementToBeClickable(By.id("BUT_757E5CE63630B7EB51350")));
        loginButton = webDriver.findElement(By.id("BUT_757E5CE63630B7EB51350"));
        loginButton.sendKeys(Keys.RETURN);

        /**
         * There might be a pop up sometimes with a finish button
         */
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("BUT_2AA03D92C0DCF9481393455")));
            WebElement cookiesButton = webDriver.findElement(By.id("BUT_2AA03D92C0DCF9481393455"));
            cookiesButton.sendKeys(Keys.RETURN);
        } catch (Exception err) {
            logger.warn("No coronavirus window found");
        }

        /**
         * New page is just a confirmation form
         * Pass over it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("BUT_02D66B31BAAF4F15580719")));
        WebElement button = webDriver.findElement(By.id("BUT_02D66B31BAAF4F15580719"));
        button.sendKeys(Keys.RETURN);

        /**
         * Go to the first account page and download the statements for the last 30 txs
         */
        List<WebElement> accountBlocks = webDriver.findElements(By.xpath("/html/body/form[1]/div[4]/div[3]/div[1]/div[2]/div[1]/div[1]/div/div[2]/div/div/div/div[1]/header"));
        List<Statement> statements= new ArrayList<>();

        List<String> accounts= new ArrayList<>();
        List<String> accountTypes= new ArrayList<>();
        List<String> linkIds= new ArrayList<>();

        for (WebElement accountBlock : accountBlocks) {
            String accountNumber = accountBlock.findElement(By.xpath("div[2]/div/div[2]/div[1]/div[2]/div/div/div[3]/div/span")).getText();
            WebElement accountIcon = accountBlock.findElement(By.xpath("div[1]/div/div/div/div/span"));
            WebElement link = accountBlock.findElement(By.xpath("div[2]/div/div[1]/div[1]/div[1]/div[1]/div/div/div[1]/div/a"));
            String accountType;
            if (accountIcon.getAttribute("data-icon").equals("C")){
                accountType= Statement.CREDIT_CARD;

            }
            else {
                accountType= Statement.DEBIT_ACCOUNT;
            }

            accountTypes.add(accountType);
            linkIds.add(link.getAttribute("id"));
            accounts.add(accountNumber);
        }
        // iterate through links
        for (int i=0; i< linkIds.size(); i++){
            String linkId = linkIds.get(i);
            WebElement link = webDriver.findElement(By.id(linkId));
            link.sendKeys(Keys.RETURN);
            wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("BUT_81752B75369FA043129962"))));
            WebElement download = webDriver.findElement(By.id("BUT_81752B75369FA043129962"));
            download.sendKeys(Keys.RETURN);
            String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
            statements.add(metroCSVConversionService.convertCSVToTransactions(accounts.get(i),
                    accountTypes.get(i), csvContent));

            /**
             * Go back home
             */
            wait.until(ExpectedConditions.elementToBeClickable(By.id("BUT_AA76E5E393F0103923812")));
            WebElement returnUrl = webDriver.findElement(By.id("BUT_AA76E5E393F0103923812"));
            returnUrl.sendKeys(Keys.RETURN);

        }
        return statements;
    }

    private void setSecurityPinEntries(WebDriver webDriver, WebDriverWait wait, int passcodeNb, String securityPin){
        char[] arraySecurityPin = securityPin.toCharArray();
        // retrieve the number that is asked by the webpage
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"security-dropdowns\"]/div/div["+passcodeNb+"]/p")));
        WebElement passCode = webDriver.findElement(By.xpath("//*[@id=\"security-dropdowns\"]/div/div["+passcodeNb+"]/p"));
        int securityPinPos= Character.getNumericValue(passCode.getText().charAt(0));
        String idForCombo="char-";
        switch (passcodeNb){
            case 1:
                idForCombo+="one";
                break;
            case 2:
                idForCombo+="two";
                break;
            case 3:
                idForCombo+="three";
                break;
        }
        // set the value to the appropriate one:
        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id(idForCombo))));
        new Select(webDriver.findElement(By.id(idForCombo))).selectByValue(String.valueOf(arraySecurityPin[securityPinPos-1]));
    }

    private void setPasswordEntries(WebDriver webDriver, WebDriverWait wait, int passwordNb, String password){
        char[] arrayPassword = password.toCharArray();
        // retrieve the number that is asked by the webpage
        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.xpath("//*[@id=\"p4_QUE_89907D559BB331AD501200\"]/div/div/div/div["+passwordNb+"]/p"))));
        WebElement passwordFieldLabel = webDriver.findElement(By.xpath("//*[@id=\"p4_QUE_89907D559BB331AD501200\"]/div/div/div/div["+passwordNb+"]/p"));

        int passwordPos= Integer.valueOf(passwordFieldLabel.getText().replaceAll("[^0-9]*", ""));

        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.xpath("//*[@id=\"Pass" + passwordNb + "_QUE_89907D559BB331AD501200\"]"))));
        WebElement passwordField = webDriver.findElement(By.xpath("//*[@id=\"Pass" + passwordNb + "_QUE_89907D559BB331AD501200\"]"));
        passwordField.sendKeys(String.valueOf(arrayPassword[passwordPos-1]));
    }


}
