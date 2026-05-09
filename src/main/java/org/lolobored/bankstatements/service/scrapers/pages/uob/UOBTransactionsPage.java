package org.lolobored.bankstatements.service.scrapers.pages.uob;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UOBTransactionsPage {

    private static final Logger logger = LoggerFactory.getLogger(UOBTransactionsPage.class);

    private static final By FREQUENCY_DROPDOWN = By.id("frequency-account-summary");
    private static final By DOWNLOAD_ICON      = By.className("i-download2");

    private static final String CURRENT_MONTH  = "Current Month";
    private static final String PREVIOUS_MONTH = "Previous Month";

    private static final int RENDER_PAUSE_MS  = 500;
    private static final int PERIOD_SETTLE_MS = 1000;

    private final WebDriver driver;
    private final WebDriverWait wait;

    public UOBTransactionsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void downloadCurrentMonth() throws InterruptedException {
        downloadForPeriod(CURRENT_MONTH);
    }

    public void downloadPreviousMonth() throws InterruptedException {
        downloadForPeriod(PREVIOUS_MONTH);
    }

    private void downloadForPeriod(String period) throws InterruptedException {
        long t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.visibilityOfElementLocated(FREQUENCY_DROPDOWN));
        logger.info("[TIMING] UOBTransactions({}): wait for frequency dropdown visible: {}ms", period, System.currentTimeMillis() - t0);

        t0 = System.currentTimeMillis();
        Thread.sleep(RENDER_PAUSE_MS);
        logger.info("[TIMING] UOBTransactions({}): post-dropdown-visible sleep (budget {}ms): {}ms", period, RENDER_PAUSE_MS, System.currentTimeMillis() - t0);

        new Select(driver.findElement(FREQUENCY_DROPDOWN)).selectByVisibleText(period);

        t0 = System.currentTimeMillis();
        Thread.sleep(PERIOD_SETTLE_MS);
        logger.info("[TIMING] UOBTransactions({}): post-period-select sleep (budget {}ms): {}ms", period, PERIOD_SETTLE_MS, System.currentTimeMillis() - t0);

        t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.visibilityOfElementLocated(DOWNLOAD_ICON));
        logger.info("[TIMING] UOBTransactions({}): wait for download icon visible: {}ms", period, System.currentTimeMillis() - t0);
        driver.findElement(DOWNLOAD_ICON).click();
    }
}
