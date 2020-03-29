package org.lolobored.bankstatements.service.scrapers.impl;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.CommBankCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.CommBankService;
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
public class CommBankServiceImpl implements CommBankService {

    private Logger logger = LoggerFactory.getLogger(CommBankServiceImpl.class);

    @Autowired
    private CommBankCSVConversionService commBankCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {

        List<Statement> statements= new ArrayList<>();

        WebDriverWait wait = new WebDriverWait(webDriver, bank.getWaitTime());
        /**
         * Delete the download directory
         */
        File downloads= new File(downloadDir);
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
        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("txtMyClientNumber_field"))));
        WebElement loginField = webDriver.findElement(By.id("txtMyClientNumber_field"));
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("txtMyPassword_field"))));
        WebElement passwordField = webDriver.findElement(By.id("txtMyPassword_field"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btnLogon_field")));
        WebElement loginButton = webDriver.findElement(By.id("btnLogon_field"));
        loginButton.sendKeys(Keys.RETURN);

        // wait until the webpage is rendered
        wait.until(ExpectedConditions.elementToBeClickable(By.id("pbsTabMenuItem")));


        List<WebElement> accountBlocks = webDriver.findElements(By.xpath("/html/body/form/div[4]/div/div[2]/div/div[2]/div[2]/div[1]/div/div/table/tbody/tr/td[1]/div/div[1]/a"));
        List<CommBankAccount> accountsDetails = new ArrayList<>();


        for (WebElement accountBlock : accountBlocks) {
            CommBankAccount account= new CommBankAccount();
            account.setUrl(accountBlock.getAttribute("href"));
            account.setAccountType(Statement.DEBIT_ACCOUNT);
            accountsDetails.add(account);
            logger.info("Collecting URL for account details " + accountBlock.getAttribute("href"));
        }

        List<WebElement> accountNumbers = webDriver.findElements(By.xpath("//html/body/form/div[4]/div/div[2]/div/div[2]/div[2]/div[1]/div/div/table/tbody/tr/td[3]/span/span"));
        for (int i=0; i< accountNumbers.size(); i++){
            String accountNumber= accountNumbers.get(i).getText().replace(" ", "");
            accountsDetails.get(i).setAccountNumber(accountNumber);
            logger.info("Collecting account number for account " + accountNumber);
        }


        for (CommBankAccount accountsDetail : accountsDetails) {
            webDriver.navigate().to(accountsDetail.getUrl());
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"cba_advanced_search_trigger\"]/i")));
            WebElement advancedButton = webDriver.findElement(By.xpath("//*[@id=\"cba_advanced_search_trigger\"]/i"));
            advancedButton.click();

            wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_BodyPlaceHolder_ddlDateRange_field")));
            Select comboBoxPeriod = new Select(webDriver.findElement(By.id("ctl00_BodyPlaceHolder_ddlDateRange_field")));
            comboBoxPeriod.selectByVisibleText("Last 120 days");
            logger.info("Selected "+comboBoxPeriod.getFirstSelectedOption().getText()+" for the period for transactions");

            Thread.sleep(1000);
            wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_BodyPlaceHolder_lbSearch")));
            WebElement searchButton = webDriver.findElement(By.id("ctl00_BodyPlaceHolder_lbSearch"));
            logger.info("Search Button selected "+searchButton);

            searchButton.sendKeys(Keys.RETURN);

            // wait up until the search was done
            logger.info("Waiting for the search to happen");
            wait.until(ExpectedConditions.invisibilityOf(webDriver.findElement(By.id("ctl00_BodyPlaceHolder_lbSearch"))));
            logger.info("Search done");

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"ctl00_CustomFooterContentPlaceHolder_updatePanelExport1\"]/div/a")));
            WebElement exportButton = webDriver.findElement(By.xpath("//*[@id=\"ctl00_CustomFooterContentPlaceHolder_updatePanelExport1\"]/div/a"));
            exportButton.sendKeys(Keys.RETURN);

            wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_CustomFooterContentPlaceHolder_ddlExportType1_field")));
            Select comboBoxExportFormat = new Select(webDriver.findElement(By.id("ctl00_CustomFooterContentPlaceHolder_ddlExportType1_field")));
            comboBoxExportFormat.selectByVisibleText("CSV (e.g. MS Excel)");

            wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_CustomFooterContentPlaceHolder_lbExport1")));
            WebElement exportAction = webDriver.findElement(By.id("ctl00_CustomFooterContentPlaceHolder_lbExport1"));
            exportAction.sendKeys(Keys.RETURN);

            String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
            statements.add(commBankCSVConversionService.convertCSVToTransactions(accountsDetail.getAccountNumber(),
                    accountsDetail.getAccountType(), csvContent));
        }


        return statements;
    }


}

@Data
class CommBankAccount{
    private String accountType;
    private String accountNumber;
    private String url;
}
