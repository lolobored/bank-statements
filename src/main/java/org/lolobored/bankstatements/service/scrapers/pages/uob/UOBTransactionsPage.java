package org.lolobored.bankstatements.service.scrapers.pages.uob;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class UOBTransactionsPage {

    private static final By FREQUENCY_DROPDOWN = By.id("frequency-account-summary");
    private static final By DOWNLOAD_ICON      = By.className("i-download2");

    private static final String CURRENT_MONTH  = "Current Month";
    private static final String PREVIOUS_MONTH = "Previous Month";

    private static final int RENDER_PAUSE_MS = 3000;

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
        wait.until(ExpectedConditions.visibilityOfElementLocated(FREQUENCY_DROPDOWN));
        Thread.sleep(RENDER_PAUSE_MS);
        new Select(driver.findElement(FREQUENCY_DROPDOWN)).selectByVisibleText(period);
        Thread.sleep(RENDER_PAUSE_MS);

        wait.until(ExpectedConditions.visibilityOfElementLocated(DOWNLOAD_ICON));
        driver.findElement(DOWNLOAD_ICON).click();
    }
}
