package org.lolobored.bankstatements.service.scrapers.pages.creditmut;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreditMutDownloadPage {

    private static final String DOWNLOAD_URL         = "https://www.creditmutuel.fr/fr/banque/compte/telechargement.cgi";
    private static final By CSV_RADIO                = By.id("csv:DataEntry");
    private static final By DOWNLOAD_BUTTON          = By.id("B3");
    private static final String ACCOUNT_CHECKBOX_FMT = "F_%d.accountCheckbox:DataEntry";

    private final WebDriver driver;
    private final WebDriverWait wait;

    public CreditMutDownloadPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void navigateToDownload() {
        driver.navigate().to(DOWNLOAD_URL);
    }

    public void selectCsvFormat() {
        driver.findElement(CSV_RADIO).click();
    }

    /** Returns the checkbox element for the given index, or null when no more accounts exist. */
    public WebElement findAccountCheckbox(int index) {
        try {
            return driver.findElement(By.id(String.format(ACCOUNT_CHECKBOX_FMT, index)));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public void clickDownload() {
        WebElement btn = driver.findElement(DOWNLOAD_BUTTON);
        wait.until(ExpectedConditions.elementToBeClickable(btn));
        new Actions(driver).moveToElement(btn).click().perform();
    }
}
