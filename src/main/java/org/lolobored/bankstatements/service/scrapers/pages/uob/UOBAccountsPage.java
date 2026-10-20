package org.lolobored.bankstatements.service.scrapers.pages.uob;

import java.io.IOException;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UOBAccountsPage {

  private static final Logger logger = LoggerFactory.getLogger(UOBAccountsPage.class);

  private static final By BACK_TO_ACCOUNTS = By.xpath("//a[normalize-space()='Accounts']");
  // Optional "Money lock" promotion shown over the dashboard, sometimes a few seconds after it
  // loads. Selectors avoid relying on the dialog markup and on the curly apostrophe in "Don’t".
  private static final By MONEY_LOCK_DONT_SHOW_AGAIN =
      By.xpath("//*[contains(normalize-space(text()), 'show this again')]");
  private static final By MONEY_LOCK_SET_LATER =
      By.xpath("//button[contains(normalize-space(), 'Set later')]");

  private static final Duration MONEY_LOCK_CHECK_WAIT = Duration.ofSeconds(5);

  private final WebDriver driver;
  private final WebDriverWait wait;

  public UOBAccountsPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  /**
   * Opens the account tile matching the given id. The dashboard shows the account number with
   * spaces (e.g. "422 323 964 6"), so the match ignores spaces and dashes; the account name (e.g.
   * "One Account") is accepted too.
   */
  public void openAccount(String accountId) throws IOException {
    String trimmed = accountId.trim();
    By accountTile =
        By.xpath(
            "//*[translate(normalize-space(text()), ' -', '')='"
                + trimmed.replaceAll("[ -]", "")
                + "' or normalize-space(text())='"
                + trimmed
                + "']");

    long t0 = System.currentTimeMillis();
    try {
      wait.until(ExpectedConditions.elementToBeClickable(accountTile));
    } catch (TimeoutException e) {
      throw new IOException("Unable to find account [" + accountId + "] on the page", e);
    }
    logger.debug(
        "[TIMING] UOBAccounts: wait for account tile clickable: {}ms",
        System.currentTimeMillis() - t0);

    dismissMoneyLockDialogIfPresent(new WebDriverWait(driver, MONEY_LOCK_CHECK_WAIT));
    try {
      driver.findElement(accountTile).click();
    } catch (ElementClickInterceptedException e) {
      // the dialog showed up after the check above
      logger.debug("UOBAccounts: account tile click intercepted, retrying after dismissing dialog");
      dismissMoneyLockDialogIfPresent(wait);
      driver.findElement(accountTile).click();
    }
  }

  public void goBackToDashboard() {
    // the download button sits far down the page and the sticky header would cover the breadcrumb
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    wait.until(ExpectedConditions.elementToBeClickable(BACK_TO_ACCOUNTS));
    driver.findElement(BACK_TO_ACCOUNTS).click();
  }

  private void dismissMoneyLockDialogIfPresent(WebDriverWait dialogWait) {
    try {
      dialogWait.until(ExpectedConditions.elementToBeClickable(MONEY_LOCK_SET_LATER));
    } catch (TimeoutException ignored) {
      logger.debug("UOBAccounts: no Money lock dialog");
      return;
    }
    // best effort: the checkbox input is styled and may not be clickable itself, so click its text
    try {
      driver.findElement(MONEY_LOCK_DONT_SHOW_AGAIN).click();
    } catch (WebDriverException e) {
      logger.debug("UOBAccounts: could not tick 'Don't show this again': {}", e.getMessage());
    }
    WebElement setLater = driver.findElement(MONEY_LOCK_SET_LATER);
    try {
      setLater.click();
    } catch (WebDriverException e) {
      ((JavascriptExecutor) driver).executeScript("arguments[0].click();", setLater);
    }
    wait.until(ExpectedConditions.invisibilityOfElementLocated(MONEY_LOCK_SET_LATER));
    logger.debug("UOBAccounts: dismissed Money lock dialog");
  }
}
