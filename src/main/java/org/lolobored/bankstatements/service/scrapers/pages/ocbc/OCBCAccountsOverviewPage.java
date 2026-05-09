package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import org.lolobored.bankstatements.utils.AccountUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class OCBCAccountsOverviewPage {

    private static final Logger logger = LoggerFactory.getLogger(OCBCAccountsOverviewPage.class);

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
    private static final Duration HOVER_PAUSE      = Duration.ofMillis(2000);
    // Short settle pause after page transitions and scroll
    private static final Duration NAVIGATION_PAUSE = Duration.ofMillis(1000);
    // Short timeout used during retry loops so we don't spend the full wait on each attempt
    private static final Duration RETRY_CHECK_WAIT = Duration.ofSeconds(5);
    private static final int MAX_HOVER_ATTEMPTS    = 3;

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
        long t0 = System.currentTimeMillis();
        Thread.sleep(NAVIGATION_PAUSE.toMillis());
        logger.info("[TIMING] OCBCOverview.navigateToOverview: initial nav sleep (budget {}ms): {}ms", NAVIGATION_PAUSE.toMillis(), System.currentTimeMillis() - t0);

        By overviewLocator = By.xpath("//*[contains(text(),'" + OVERVIEW_LABEL + "')]");
        WebDriverWait shortWait = new WebDriverWait(driver, RETRY_CHECK_WAIT);

        for (int attempt = 0; attempt < MAX_HOVER_ATTEMPTS; attempt++) {
            t0 = System.currentTimeMillis();
            wait.until(ExpectedConditions.visibilityOfElementLocated(VIEW_ACCOUNTS_NAV));
            logger.info("[TIMING] OCBCOverview.navigateToOverview: wait for VIEW_ACCOUNTS_NAV (attempt {}): {}ms", attempt, System.currentTimeMillis() - t0);

            WebElement viewAccounts = driver.findElement(VIEW_ACCOUNTS_NAV);
            t0 = System.currentTimeMillis();
            new Actions(driver).moveToElement(viewAccounts).pause(HOVER_PAUSE).build().perform();
            logger.info("[TIMING] OCBCOverview.navigateToOverview: hover+pause action (budget {}ms, attempt {}): {}ms", HOVER_PAUSE.toMillis(), attempt, System.currentTimeMillis() - t0);

            try {
                t0 = System.currentTimeMillis();
                shortWait.until(ExpectedConditions.elementToBeClickable(overviewLocator));
                logger.info("[TIMING] OCBCOverview.navigateToOverview: wait for Overview clickable (attempt {}): {}ms", attempt, System.currentTimeMillis() - t0);
                driver.findElement(overviewLocator).click();
                return;
            } catch (TimeoutException ignored) {
                logger.info("[TIMING] OCBCOverview.navigateToOverview: Overview not found after hover (attempt {}), retrying", attempt);
                // hover didn't expose the submenu — move mouse away and retry
                new Actions(driver).moveToElement(driver.findElement(PAGE_TOP_ANCHOR)).build().perform();
                t0 = System.currentTimeMillis();
                Thread.sleep(NAVIGATION_PAUSE.toMillis());
                logger.info("[TIMING] OCBCOverview.navigateToOverview: retry reset sleep (budget {}ms): {}ms", NAVIGATION_PAUSE.toMillis(), System.currentTimeMillis() - t0);
            }
        }

        // All retries exhausted — final attempt with full timeout for a meaningful error
        wait.until(ExpectedConditions.elementToBeClickable(overviewLocator));
        driver.findElement(overviewLocator).click();
    }

    public void scrollToTop() {
        WebElement top = driver.findElement(PAGE_TOP_ANCHOR);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", top);
    }

    private void openAccount(By accountsBlockLocator, String accountName) throws Exception {
        long t0 = System.currentTimeMillis();
        wait.until(ExpectedConditions.visibilityOfElementLocated(accountsBlockLocator));
        logger.info("[TIMING] OCBCOverview.openAccount: wait for accounts block visible: {}ms", System.currentTimeMillis() - t0);

        WebElement accountsBlock = driver.findElement(accountsBlockLocator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", accountsBlock);

        t0 = System.currentTimeMillis();
        Thread.sleep(NAVIGATION_PAUSE.toMillis());
        logger.info("[TIMING] OCBCOverview.openAccount: post-scroll sleep (budget {}ms): {}ms", NAVIGATION_PAUSE.toMillis(), System.currentTimeMillis() - t0);

        List<WebElement> accountRows = accountsBlock.findElements(ACCOUNT_ROW);
        for (WebElement row : accountRows) {
            if (accountName.equals(AccountUtils.getCleanedAccount(row.getText()))) {
                WebDriverWait shortWait = new WebDriverWait(driver, RETRY_CHECK_WAIT);

                for (int attempt = 0; attempt < MAX_HOVER_ATTEMPTS; attempt++) {
                    t0 = System.currentTimeMillis();
                    new Actions(driver).moveToElement(row).pause(HOVER_PAUSE).build().perform();
                    logger.info("[TIMING] OCBCOverview.openAccount: hover+pause action (budget {}ms, attempt {}): {}ms", HOVER_PAUSE.toMillis(), attempt, System.currentTimeMillis() - t0);

                    try {
                        t0 = System.currentTimeMillis();
                        shortWait.until(ExpectedConditions.visibilityOfElementLocated(ACCOUNT_ACTION_LINKS));
                        logger.info("[TIMING] OCBCOverview.openAccount: wait for action links visible (attempt {}): {}ms", attempt, System.currentTimeMillis() - t0);
                        List<WebElement> actionLinks = driver.findElements(ACCOUNT_ACTION_LINKS);
                        for (WebElement link : actionLinks) {
                            if (DETAILS_TRANSACTIONS_LABEL.equals(link.getText())) {
                                link.click();
                                return;
                            }
                        }
                    } catch (TimeoutException ignored) {
                        logger.info("[TIMING] OCBCOverview.openAccount: action links not visible after hover (attempt {}), retrying", attempt);
                        // hover didn't reveal action links — retry
                    }
                }

                throw new IOException("Could not find '" + DETAILS_TRANSACTIONS_LABEL + "' link for account: " + accountName);
            }
        }
        throw new IOException("Could not find account: " + accountName);
    }
}
