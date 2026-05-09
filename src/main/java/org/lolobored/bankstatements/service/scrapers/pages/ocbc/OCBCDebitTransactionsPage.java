package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OCBCDebitTransactionsPage {

  private static final Logger logger = LoggerFactory.getLogger(OCBCDebitTransactionsPage.class);

  private static final By DOWNLOAD_BUTTON = By.xpath("//*[contains(text(),'Download')]");
  private static final By CSV_OPTION = By.xpath("//*[contains(text(),'CSV')]");

  private static final int RENDER_PAUSE_MS = 500;

  private final WebDriver driver;
  private final WebDriverWait wait;

  public OCBCDebitTransactionsPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void downloadCsv() throws InterruptedException {
    long t0 = System.currentTimeMillis();
    Thread.sleep(RENDER_PAUSE_MS);
    logger.debug(
        "[TIMING] OCBCDebit: initial render sleep (budget {}ms): {}ms",
        RENDER_PAUSE_MS,
        System.currentTimeMillis() - t0);

    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.elementToBeClickable(DOWNLOAD_BUTTON));
    logger.debug(
        "[TIMING] OCBCDebit: wait for download button clickable: {}ms",
        System.currentTimeMillis() - t0);

    WebElement download = driver.findElement(DOWNLOAD_BUTTON);
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", download);
    download.click();

    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.visibilityOfElementLocated(CSV_OPTION));
    logger.debug(
        "[TIMING] OCBCDebit: wait for CSV option visible: {}ms", System.currentTimeMillis() - t0);
    driver.findElement(CSV_OPTION).click();
  }
}
