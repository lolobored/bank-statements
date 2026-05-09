package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OCBCCreditTransactionsPage {

    private static final By PERIOD_DROPDOWN = By.id("dropdown-cards-list");
    private static final By DOWNLOAD_BUTTON = By.xpath("//*[contains(text(),'Download')]");
    private static final By CSV_OPTION      = By.xpath("//*[contains(text(),'CSV')]");

    // Label of the period option to select in the dropdown
    private static final By PAST_30_DAYS   = By.xpath("//*[contains(text(),'Past 30 Days')]");

    private static final int RENDER_PAUSE_MS = 3000;

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OCBCCreditTransactionsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void downloadCsv() throws InterruptedException {
        Thread.sleep(RENDER_PAUSE_MS);

        wait.until(ExpectedConditions.presenceOfElementLocated(PERIOD_DROPDOWN));
        WebElement dropdown = driver.findElement(PERIOD_DROPDOWN);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", dropdown);
        Thread.sleep(RENDER_PAUSE_MS);
        dropdown.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(PAST_30_DAYS));
        driver.findElement(PAST_30_DAYS).click();
        Thread.sleep(RENDER_PAUSE_MS);

        wait.until(ExpectedConditions.presenceOfElementLocated(DOWNLOAD_BUTTON));
        WebElement download = driver.findElement(DOWNLOAD_BUTTON);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", download);
        Thread.sleep(RENDER_PAUSE_MS);
        download.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(CSV_OPTION));
        driver.findElement(CSV_OPTION).click();
    }
}
