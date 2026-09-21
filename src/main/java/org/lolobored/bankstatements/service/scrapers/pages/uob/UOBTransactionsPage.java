package org.lolobored.bankstatements.service.scrapers.pages.uob;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UOBTransactionsPage {

  private static final Logger logger = LoggerFactory.getLogger(UOBTransactionsPage.class);

  private static final By TRANSACTIONS_TAB =
      By.xpath("//button[@role='tab' and normalize-space()='Transactions']");
  private static final By DOWNLOAD_EXCEL_BUTTON =
      By.xpath("//button[normalize-space()='Download as Excel']");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public UOBTransactionsPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  /**
   * Downloads the Excel statement for the default transaction period, which UOB sets to the last 60
   * days (i.e. always covers the current and previous month).
   */
  public void downloadStatement() {
    // narrow windows show Details / Transactions tabs; wide windows stack both sections on one page
    // and have no tabs, in which case the download button is already on the page
    wait.until(
        ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(TRANSACTIONS_TAB),
            ExpectedConditions.presenceOfElementLocated(DOWNLOAD_EXCEL_BUTTON)));
    List<WebElement> tabs = driver.findElements(TRANSACTIONS_TAB);
    if (!tabs.isEmpty()) {
      logger.debug("UOBTransactions: tabbed layout, opening Transactions tab");
      tabs.get(0).click();
    } else {
      logger.debug("UOBTransactions: single-page layout, no Transactions tab");
    }

    long t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.elementToBeClickable(DOWNLOAD_EXCEL_BUTTON));
    logger.debug(
        "[TIMING] UOBTransactions: wait for Download as Excel clickable: {}ms",
        System.currentTimeMillis() - t0);
    driver.findElement(DOWNLOAD_EXCEL_BUTTON).click();
  }
}
