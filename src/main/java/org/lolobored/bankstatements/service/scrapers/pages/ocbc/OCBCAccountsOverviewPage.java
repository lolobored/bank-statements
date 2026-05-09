package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import org.lolobored.bankstatements.utils.AccountUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class OCBCAccountsOverviewPage {

    // Debit accounts are rendered inside a Material UI paper block
    private static final By DEBIT_ACCOUNTS_BLOCK  = By.className("mfe-cfo-portfolio--MuiPaper-root");
    // Credit accounts are rendered inside the classic OCBC container
    private static final By CREDIT_ACCOUNTS_BLOCK = By.className("wyoMainContainer");
    // Each account row is a hoverable element
    private static final By ACCOUNT_ROW           = By.className("cursor");
    // The action links that appear on hover (Details, Statements, etc.)
    private static final By ACCOUNT_ACTION_LINKS  = By.className("first");
    // Nav link that reveals the accounts sub-menu on hover
    private static final By VIEW_ACCOUNTS_NAV     = By.xpath("//*[contains(text(),'View accounts')]");
    // Anchor used to scroll back to the top of the page
    private static final By PAGE_TOP_ANCHOR       = By.id("content");

    private static final String DETAILS_TRANSACTIONS_LABEL = "Details / Transactions";
    private static final String OVERVIEW_LABEL             = "Overview";

    // Hover menus need time to animate in
    private static final Duration HOVER_PAUSE       = Duration.ofMillis(3000);
    private static final Duration NAVIGATION_PAUSE  = Duration.ofMillis(3000);

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OCBCAccountsOverviewPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void openDebitAccount(String accountName) throws Exception {
        openAccount(DEBIT_ACCOUNTS_BLOCK, accountName);
    }

    public void openCreditAccount(String accountName) throws Exception {
        openAccount(CREDIT_ACCOUNTS_BLOCK, accountName);
    }

    public void navigateToOverview() throws InterruptedException {
        Thread.sleep(NAVIGATION_PAUSE.toMillis());

        wait.until(ExpectedConditions.visibilityOfElementLocated(VIEW_ACCOUNTS_NAV));
        WebElement viewAccounts = driver.findElement(VIEW_ACCOUNTS_NAV);
        new Actions(driver).moveToElement(viewAccounts).pause(HOVER_PAUSE).build().perform();
        Thread.sleep(NAVIGATION_PAUSE.toMillis());

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(),'" + OVERVIEW_LABEL + "')]")));
        driver.findElement(By.xpath("//*[contains(text(),'" + OVERVIEW_LABEL + "')]")).click();
    }

    public void scrollToTop() {
        WebElement top = driver.findElement(PAGE_TOP_ANCHOR);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", top);
    }

    private void openAccount(By accountsBlockLocator, String accountName) throws Exception {
        wait.until(ExpectedConditions.visibilityOfElementLocated(accountsBlockLocator));
        WebElement accountsBlock = driver.findElement(accountsBlockLocator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", accountsBlock);
        Thread.sleep(HOVER_PAUSE.toMillis());

        List<WebElement> accountRows = accountsBlock.findElements(ACCOUNT_ROW);
        for (WebElement row : accountRows) {
            if (accountName.equals(AccountUtils.getCleanedAccount(row.getText()))) {
                new Actions(driver).moveToElement(row).pause(HOVER_PAUSE).build().perform();

                List<WebElement> actionLinks = driver.findElements(ACCOUNT_ACTION_LINKS);
                for (WebElement link : actionLinks) {
                    if (DETAILS_TRANSACTIONS_LABEL.equals(link.getText())) {
                        link.click();
                        return;
                    }
                }
                throw new IOException("Could not find '" + DETAILS_TRANSACTIONS_LABEL + "' link for account: " + accountName);
            }
        }
        throw new IOException("Could not find account: " + accountName);
    }
}
