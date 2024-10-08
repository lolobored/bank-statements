package org.lolobored.bankstatements.service.scrapers.impl;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.CommBankCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.CommBankService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommBankServiceImpl implements CommBankService {

  private Logger logger = LoggerFactory.getLogger(CommBankServiceImpl.class);

  @Autowired
  private CommBankCSVConversionService commBankCSVConversionService;

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
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("txtMyClientNumber_field")));
    WebElement loginField = webDriver.findElement(By.id("txtMyClientNumber_field"));
    loginField.sendKeys(bank.getUsername());

    logger.info("Username field [" + bank.getUsername() + "]");

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("txtMyPassword_field")));
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

    // deal with the cookie
    try {
      wait.until(ExpectedConditions.elementToBeClickable(By.id("tt_prominenceButtonNo")));
      WebElement cookieButton = webDriver.findElement(By.id("tt_prominenceButtonNo"));
      cookieButton.sendKeys(Keys.RETURN);
    } catch (TimeoutException donothing) {
    }

    List<WebElement> accountBlocks = webDriver.findElements(By.className("account-card"));
    List<CommBankAccount> accountsDetails = new ArrayList<>();


    for (WebElement accountBlock : accountBlocks) {
      CommBankAccount account = new CommBankAccount();
      String accountNumber = accountBlock.findElement(By.className("account-number")).getText();
      accountNumber = StringUtils.substringAfter(accountNumber, " ");
      accountNumber = accountNumber.replace(" ", "");
      account.setAccountNumber(accountNumber);
      logger.info("Collecting account number for account " + accountNumber);
      // get action button
      WebElement actionButton = accountBlock.findElement(By.className("options-button"));
      actionButton.click();

      // once clicked we can retrieve the url
      List<WebElement> viewTxsLink = accountBlock.findElements(By.className("btn-action--subtle"));
      for (WebElement viewTxLink : viewTxsLink) {
        if ("View Transactions".equalsIgnoreCase(viewTxLink.getText())) {
          String url = viewTxLink.getAttribute("href");
          account.setUrl(url);
          logger.info("Collecting URL for account details " + url);
          WebElement cancelButton = accountBlock.findElement(By.className("cancel-button"));
          cancelButton.click();
          break;
        }
      }

      account.setAccountType(Statement.DEBIT_ACCOUNT);
      accountsDetails.add(account);

    }

    for (CommBankAccount accountsDetail : accountsDetails) {
      webDriver.navigate().to(accountsDetail.getUrl());
      try {
        logger.info("Starting download for "+accountsDetail.getAccountNumber());
        // that would be the former layout
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"cba_advanced_search_trigger\"]/i")));
        WebElement advancedButton = webDriver.findElement(By.xpath("//*[@id=\"cba_advanced_search_trigger\"]/i"));
        ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", advancedButton);
        Actions actionsKeys = new Actions(webDriver);
        actionsKeys.moveToElement(advancedButton).click().perform();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_BodyPlaceHolder_ddlDateRange_field")));
        Select comboBoxPeriod = new Select(webDriver.findElement(By.id("ctl00_BodyPlaceHolder_ddlDateRange_field")));
        comboBoxPeriod.selectByVisibleText("Last 120 days");
        logger.info("Selected " + comboBoxPeriod.getFirstSelectedOption().getText() + " for the period for transactions");

        Thread.sleep(1000);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_BodyPlaceHolder_lbSearch")));
        WebElement searchButton = webDriver.findElement(By.id("ctl00_BodyPlaceHolder_lbSearch"));
        logger.info("Search Button selected " + searchButton);

        wait.until(ExpectedConditions.elementToBeClickable(searchButton));
        Thread.sleep(1000);
        searchButton.sendKeys(Keys.RETURN);
        // wait up until the search was done
        logger.info("Waiting for the search to happen");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("ctl00_BodyPlaceHolder_lbSearch")));
        logger.info("Search done");

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"ctl00_CustomFooterContentPlaceHolder_updatePanelExport1\"]/div/a")));
        WebElement exportButton = webDriver.findElement(By.xpath("//*[@id=\"ctl00_CustomFooterContentPlaceHolder_updatePanelExport1\"]/div/a"));
        Actions actions = new Actions(webDriver);
        actions.moveToElement(exportButton).click().perform();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_CustomFooterContentPlaceHolder_ddlExportType1_field")));
        Select comboBoxExportFormat = new Select(webDriver.findElement(By.id("ctl00_CustomFooterContentPlaceHolder_ddlExportType1_field")));
        comboBoxExportFormat.selectByVisibleText("CSV (e.g. MS Excel)");

        wait.until(ExpectedConditions.elementToBeClickable(By.id("ctl00_CustomFooterContentPlaceHolder_lbExport1")));
        WebElement exportAction = webDriver.findElement(By.id("ctl00_CustomFooterContentPlaceHolder_lbExport1"));
        actions = new Actions(webDriver);
        actions.moveToElement(exportAction).click().perform();
      } catch (TimeoutException potentialNewTransaction) {
        // this is hopefully the new layout
        // now the search box is not going so far away but just using the default for now
        wait.until(ExpectedConditions.elementToBeClickable(By.id("export-link")));
        WebElement exportButton = webDriver.findElement(By.id("export-link"));
        ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", exportButton);
        Actions actions = new Actions(webDriver);
        actions.moveToElement(exportButton).click().perform();

        // choose the CSV download option
        WebElement exportFormatTypes = webDriver.findElement(By.id("export-format-type"));
        WebElement csvButton = exportFormatTypes.findElement(By.id("export-format-type-CSV"));
        ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", csvButton);
        ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", csvButton);

        // click on the download button
        WebElement downloadButton = webDriver.findElement(By.id("txnListExport-submit-btn"));
        ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", downloadButton);
        actions = new Actions(webDriver);
        actions.moveToElement(downloadButton).click().perform();
      }

      String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
      statements.add(commBankCSVConversionService.convertTableToTransactions(accountsDetail.getAccountNumber(),
              accountsDetail.getAccountType(), csvContent));
    }


    return statements;
  }


}

@Data
class CommBankAccount {
  private String accountType;
  private String accountNumber;
  private String url;
}
