package org.lolobored.bankstatements.service.scrapers.pages.westpac;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WestpacExportPage {

  private static final String EXPORT_URL =
      "https://banking.westpac.com.au/secure/banking/reportsandexports/exportparameters/2/";
  private static final By DATE_FROM_FIELD = By.id("DateRange_StartDate");
  private static final By SELECT_MULTIPLE = By.className("select-multiple");
  private static final By SELECT_ALL = By.id("_selectall");
  private static final By SUBMIT_BUTTON = By.className("btn-submit");
  private static final By DOWNLOAD_LINK = By.className("export-link");

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public WestpacExportPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void navigateToExport() {
    driver.navigate().to(EXPORT_URL);
  }

  public void downloadCsv() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(DATE_FROM_FIELD));
    WebElement dateFrom = driver.findElement(DATE_FROM_FIELD);
    dateFrom.clear();
    dateFrom.sendKeys(LocalDate.now().minusMonths(1).format(DATE_FORMAT));

    driver.findElement(SELECT_MULTIPLE).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(SELECT_ALL));
    driver.findElement(SELECT_ALL).click();

    driver.findElement(SUBMIT_BUTTON).click();

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    driver.findElement(DOWNLOAD_LINK).click();
  }
}
