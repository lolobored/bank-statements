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

  private static final String DASHBOARD_URL_FRAGMENT = "accountsDashboard";

  // The login form is a micro-frontend that UOB sometimes fails to load (the client-side loader
  // times out after ~30s), leaving a blank page that never recovers: reload it when that happens
  private static final Duration FORM_LOAD_TIMEOUT = Duration.ofSeconds(40);
  private static final int MAX_FORM_LOAD_ATTEMPTS = 3;

  private final WebDriver driver;
  private final WebDriverWait wait;
  private final Duration formLoadTimeout;

  public UOBLoginPage(WebDriver driver, WebDriverWait wait) {
    this(driver, wait, FORM_LOAD_TIMEOUT);
  }

  UOBLoginPage(WebDriver driver, WebDriverWait wait, Duration formLoadTimeout) {
    this.driver = driver;
    this.wait = wait;
    this.formLoadTimeout = formLoadTimeout;
  }

  public void login(String url, String username, String password) {
    long t0 = System.currentTimeMillis();
    openLoginForm(url);
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
  }

  private void openLoginForm(String url) {
    WebDriverWait formWait = new WebDriverWait(driver, formLoadTimeout);
    for (int attempt = 1; ; attempt++) {
      driver.get(url);
      try {
        formWait.until(
            ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD),
                ExpectedConditions.elementToBeClickable(LOG_IN_AGAIN_BUTTON)));
        return;
      } catch (TimeoutException e) {
        if (attempt == MAX_FORM_LOAD_ATTEMPTS) {
          throw e;
        }
        logger.warn(
            "UOBLogin: login form did not render (attempt {}/{}), reloading",
            attempt,
            MAX_FORM_LOAD_ATTEMPTS);
      }
    }
  }
}
