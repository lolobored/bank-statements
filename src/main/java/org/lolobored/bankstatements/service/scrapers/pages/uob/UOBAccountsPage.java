package org.lolobored.bankstatements.service.scrapers.pages.uob;

import java.io.IOException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UOBAccountsPage {

  private static final Logger logger = LoggerFactory.getLogger(UOBAccountsPage.class);

  private static final By BACK_TO_ACCOUNTS = By.xpath("//a[normalize-space()='Accounts']");

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
    driver.findElement(accountTile).click();
  }

  public void goBackToDashboard() {
    // the download button sits far down the page and the sticky header would cover the breadcrumb
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    wait.until(ExpectedConditions.elementToBeClickable(BACK_TO_ACCOUNTS));
    driver.findElement(BACK_TO_ACCOUNTS).click();
  }
}
