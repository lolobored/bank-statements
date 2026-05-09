package org.lolobored.bankstatements.service.scrapers.pages.uob;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class UOBAccountsPage {

    private static final Logger logger = LoggerFactory.getLogger(UOBAccountsPage.class);

    private static final By ACCOUNT_TILES  = By.className("color-account");
    private static final By BACK_DASHBOARD = By.className("uob-dashboard");

    private static final int ACCOUNT_CLICK_PAUSE_MS = 500;

    private final WebDriver driver;
    private final WebDriverWait wait;

    public UOBAccountsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void openAccount(String accountId) throws IOException, InterruptedException {
        long t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.visibilityOfElementLocated(ACCOUNT_TILES));
        logger.debug("[TIMING] UOBAccounts: wait for account tiles visible: {}ms", System.currentTimeMillis() - t0);

        List<WebElement> accounts = driver.findElements(ACCOUNT_TILES);

        for (WebElement account : accounts) {
            if (StringUtils.equalsIgnoreCase(accountId.trim(), account.getText().trim())) {
                t0 = System.currentTimeMillis();
                Thread.sleep(ACCOUNT_CLICK_PAUSE_MS);
                logger.debug("[TIMING] UOBAccounts: pre-click sleep (budget {}ms): {}ms", ACCOUNT_CLICK_PAUSE_MS, System.currentTimeMillis() - t0);
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
