package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OCBCCreditTransactionsPage {

    private static final Logger logger = LoggerFactory.getLogger(OCBCCreditTransactionsPage.class);

    private static final By PERIOD_DROPDOWN  = By.id("dropdown-cards-list");
    private static final By DOWNLOAD_BUTTON  = By.xpath("//*[contains(text(),'Download')]");
    private static final By CSV_OPTION       = By.xpath("//*[contains(text(),'CSV')]");
    private static final By LOADING_BACKDROP = By.className("app-loader-backdrop");

    // Label of the period option to select in the dropdown
    private static final By PAST_30_DAYS   = By.xpath("//*[contains(text(),'Past 30 Days')]");

    private static final int RENDER_PAUSE_MS = 500;

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OCBCCreditTransactionsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void downloadCsv() throws InterruptedException {
        long t0 = System.currentTimeMillis();
        Thread.sleep(RENDER_PAUSE_MS);
        logger.info("[TIMING] OCBCCredit: initial render sleep (budget {}ms): {}ms", RENDER_PAUSE_MS, System.currentTimeMillis() - t0);

        t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.elementToBeClickable(PERIOD_DROPDOWN));
        logger.info("[TIMING] OCBCCredit: wait for period dropdown clickable: {}ms", System.currentTimeMillis() - t0);

        WebElement dropdown = driver.findElement(PERIOD_DROPDOWN);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", dropdown);
        dropdown.click();

        t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.visibilityOfElementLocated(PAST_30_DAYS));
        logger.info("[TIMING] OCBCCredit: wait for PAST_30_DAYS option visible: {}ms", System.currentTimeMillis() - t0);
        driver.findElement(PAST_30_DAYS).click();

        // Brief pause so the loading backdrop has time to appear before we check for it
        Thread.sleep(300);
        t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING_BACKDROP));
        logger.info("[TIMING] OCBCCredit: wait for loading backdrop gone: {}ms", System.currentTimeMillis() - t0);

        t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.elementToBeClickable(DOWNLOAD_BUTTON));
        logger.info("[TIMING] OCBCCredit: wait for download button clickable: {}ms", System.currentTimeMillis() - t0);

        WebElement download = driver.findElement(DOWNLOAD_BUTTON);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", download);
        download.click();

        t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.visibilityOfElementLocated(CSV_OPTION));
        logger.info("[TIMING] OCBCCredit: wait for CSV option visible: {}ms", System.currentTimeMillis() - t0);
        driver.findElement(CSV_OPTION).click();
    }
}
