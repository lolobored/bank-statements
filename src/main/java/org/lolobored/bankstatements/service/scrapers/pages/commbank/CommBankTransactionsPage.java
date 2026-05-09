package org.lolobored.bankstatements.service.scrapers.pages.commbank;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CommBankTransactionsPage {

  // Legacy layout selectors
  private static final By OLD_ADVANCED_BUTTON =
      By.xpath("//*[@id=\"cba_advanced_search_trigger\"]/i");
  private static final By OLD_DATE_RANGE = By.id("ctl00_BodyPlaceHolder_ddlDateRange_field");
  private static final By OLD_SEARCH_BUTTON = By.id("ctl00_BodyPlaceHolder_lbSearch");
  private static final By OLD_EXPORT_BUTTON =
      By.xpath("//*[@id=\"ctl00_CustomFooterContentPlaceHolder_updatePanelExport1\"]/div/a");
  private static final By OLD_EXPORT_FORMAT =
      By.id("ctl00_CustomFooterContentPlaceHolder_ddlExportType1_field");
  private static final By OLD_EXPORT_ACTION =
      By.id("ctl00_CustomFooterContentPlaceHolder_lbExport1");

  // New layout selectors
  private static final By NEW_EXPORT_LINK = By.id("export-link");
  private static final By NEW_EXPORT_FORMAT = By.id("export-format-type");
  private static final By NEW_CSV_BUTTON = By.id("export-format-type-CSV");
  private static final By NEW_DOWNLOAD_BUTTON = By.id("txnListExport-submit-btn");

  private static final String LAST_120_DAYS = "Last 120 days";
  private static final String CSV_FORMAT = "CSV (e.g. MS Excel)";

  private final WebDriver driver;
  private final WebDriverWait wait;

  public CommBankTransactionsPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void navigateTo(String url) {
    driver.navigate().to(url);
  }

  public void downloadCsv() throws InterruptedException {
    try {
      downloadOldLayout();
    } catch (TimeoutException e) {
      downloadNewLayout();
    }
  }

  private void downloadOldLayout() throws InterruptedException {
    wait.until(ExpectedConditions.elementToBeClickable(OLD_ADVANCED_BUTTON));
    WebElement advancedButton = driver.findElement(OLD_ADVANCED_BUTTON);
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView(true);", advancedButton);
    new Actions(driver).moveToElement(advancedButton).click().perform();

    wait.until(ExpectedConditions.elementToBeClickable(OLD_DATE_RANGE));
    new Select(driver.findElement(OLD_DATE_RANGE)).selectByVisibleText(LAST_120_DAYS);

    Thread.sleep(1000);
    wait.until(ExpectedConditions.elementToBeClickable(OLD_SEARCH_BUTTON));
    WebElement searchButton = driver.findElement(OLD_SEARCH_BUTTON);
    wait.until(ExpectedConditions.elementToBeClickable(searchButton));
    Thread.sleep(1000);
    searchButton.sendKeys(Keys.RETURN);
    wait.until(ExpectedConditions.invisibilityOfElementLocated(OLD_SEARCH_BUTTON));

    wait.until(ExpectedConditions.elementToBeClickable(OLD_EXPORT_BUTTON));
    new Actions(driver).moveToElement(driver.findElement(OLD_EXPORT_BUTTON)).click().perform();

    wait.until(ExpectedConditions.elementToBeClickable(OLD_EXPORT_FORMAT));
    new Select(driver.findElement(OLD_EXPORT_FORMAT)).selectByVisibleText(CSV_FORMAT);

    wait.until(ExpectedConditions.elementToBeClickable(OLD_EXPORT_ACTION));
    new Actions(driver).moveToElement(driver.findElement(OLD_EXPORT_ACTION)).click().perform();
  }

  private void downloadNewLayout() {
    wait.until(ExpectedConditions.elementToBeClickable(NEW_EXPORT_LINK));
    WebElement exportButton = driver.findElement(NEW_EXPORT_LINK);
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", exportButton);
    new Actions(driver).moveToElement(exportButton).click().perform();

    WebElement csvButton = driver.findElement(NEW_EXPORT_FORMAT).findElement(NEW_CSV_BUTTON);
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", csvButton);
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", csvButton);

    WebElement downloadButton = driver.findElement(NEW_DOWNLOAD_BUTTON);
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView(true);", downloadButton);
    new Actions(driver).moveToElement(downloadButton).click().perform();
  }
}
