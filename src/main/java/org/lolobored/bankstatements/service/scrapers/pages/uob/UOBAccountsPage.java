package org.lolobored.bankstatements.service.scrapers.pages.uob;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.util.List;

public class UOBAccountsPage {

    private static final By ACCOUNT_TILES  = By.className("color-account");
    private static final By BACK_DASHBOARD = By.className("uob-dashboard");

    private static final int ACCOUNT_CLICK_PAUSE_MS = 3000;

    private final WebDriver driver;
    private final WebDriverWait wait;

    public UOBAccountsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void openAccount(String accountId) throws IOException, InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(ACCOUNT_TILES));
        List<WebElement> accounts = driver.findElements(ACCOUNT_TILES);

        for (WebElement account : accounts) {
            if (StringUtils.equalsIgnoreCase(accountId.trim(), account.getText().trim())) {
                Thread.sleep(ACCOUNT_CLICK_PAUSE_MS);
                account.click();
                return;
            }
        }

        throw new IOException("Unable to find account [" + accountId + "] on the page");
    }

    public void goBackToDashboard() {
        driver.findElement(BACK_DASHBOARD).click();
    }
}
