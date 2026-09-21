package org.lolobored.bankstatements.service.scrapers.pages.uob;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UOBLoginPage {

  private static final Logger logger = LoggerFactory.getLogger(UOBLoginPage.class);

  private static final By USERNAME_FIELD = By.cssSelector("input[placeholder='USERNAME']");
  private static final By PASSWORD_FIELD = By.cssSelector("input[placeholder='PASSWORD']");
  private static final By LOGIN_BUTTON = By.xpath("//button[normalize-space()='Log in']");
  // UOB shows this page when a login is detected from another device or browser
  private static final By LOG_IN_AGAIN_BUTTON =
      By.xpath("//button[normalize-space()='Log in again']");
  // Optional "Money lock" promotion shown on the dashboard after login
  private static final By MONEY_LOCK_DONT_SHOW_AGAIN =
      By.xpath("//div[@role='dialog']//input[@type='checkbox']");
  private static final By MONEY_LOCK_SET_LATER =
      By.xpath("//div[@role='dialog']//button[contains(normalize-space(), 'Set later')]");

  private static final String DASHBOARD_URL_FRAGMENT = "accountsDashboard";
  private static final Duration MONEY_LOCK_CHECK_WAIT = Duration.ofSeconds(5);

  private final WebDriver driver;
  private final WebDriverWait wait;

  public UOBLoginPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void login(String url, String username, String password) {
    driver.get(url);

    long t0 = System.currentTimeMillis();
    wait.until(
        ExpectedConditions.or(
            ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD),
            ExpectedConditions.elementToBeClickable(LOG_IN_AGAIN_BUTTON)));
    logger.debug("[TIMING] UOBLogin: wait for login form: {}ms", System.currentTimeMillis() - t0);

    if (!driver.findElements(LOG_IN_AGAIN_BUTTON).isEmpty()) {
      logger.debug("UOBLogin: logged-out page shown, clicking 'Log in again'");
      driver.findElement(LOG_IN_AGAIN_BUTTON).click();
      wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD));
    }

    driver.findElement(USERNAME_FIELD).sendKeys(username);
    driver.findElement(PASSWORD_FIELD).sendKeys(password);

    // the button stays disabled until both fields are filled
    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
    logger.debug(
        "[TIMING] UOBLogin: wait for login button clickable: {}ms",
        System.currentTimeMillis() - t0);
    driver.findElement(LOGIN_BUTTON).click();

    // a "Confirm access" dialog waits here for the push notification approval on the phone
    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.urlContains(DASHBOARD_URL_FRAGMENT));
    logger.debug(
        "[TIMING] UOBLogin: wait for MFA approval and dashboard: {}ms",
        System.currentTimeMillis() - t0);

    dismissMoneyLockDialogIfPresent();
  }

  private void dismissMoneyLockDialogIfPresent() {
    try {
      new WebDriverWait(driver, MONEY_LOCK_CHECK_WAIT)
          .until(ExpectedConditions.elementToBeClickable(MONEY_LOCK_SET_LATER));
      driver.findElement(MONEY_LOCK_DONT_SHOW_AGAIN).click();
      driver.findElement(MONEY_LOCK_SET_LATER).click();
      logger.debug("UOBLogin: dismissed Money lock dialog");
    } catch (TimeoutException ignored) {
      logger.debug("UOBLogin: no Money lock dialog");
    }
  }
}
