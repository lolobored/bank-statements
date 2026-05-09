package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OCBCDebitTransactionsPage {

    private static final By DOWNLOAD_BUTTON = By.xpath("//*[contains(text(),'Download')]");
    private static final By CSV_OPTION      = By.xpath("//*[contains(text(),'CSV')]");

    private static final int RENDER_PAUSE_MS = 3000;

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OCBCDebitTransactionsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void downloadCsv() throws InterruptedException {
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
