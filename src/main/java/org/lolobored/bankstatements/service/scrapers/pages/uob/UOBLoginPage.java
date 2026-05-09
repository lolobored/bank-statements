package org.lolobored.bankstatements.service.scrapers.pages.uob;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UOBLoginPage {

  private static final Logger logger = LoggerFactory.getLogger(UOBLoginPage.class);

  private static final By USERNAME_FIELD = By.id("userName");
  private static final By PASSWORD_FIELD = By.id("PASSWORD1");
  private static final By LOGIN_BUTTON = By.id("btnSubmit");
  // UOB shows this dialog when a previous session is still active
  private static final By PROCEED_BUTTON = By.xpath("//button[normalize-space()='Proceed']");

  private static final Duration PROCEED_CHECK_WAIT = Duration.ofSeconds(10);

  private final WebDriver driver;
  private final WebDriverWait wait;

  public UOBLoginPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void login(String url, String username, String password) {
    driver.get(url);

    long t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD));
    logger.debug(
        "[TIMING] UOBLogin: wait for username field visible: {}ms",
        System.currentTimeMillis() - t0);
    driver.findElement(USERNAME_FIELD).sendKeys(username);

    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
    logger.debug(
        "[TIMING] UOBLogin: wait for password field visible: {}ms",
        System.currentTimeMillis() - t0);
    driver.findElement(PASSWORD_FIELD).sendKeys(password);

    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
    logger.debug(
        "[TIMING] UOBLogin: wait for login button clickable: {}ms",
        System.currentTimeMillis() - t0);
    driver.findElement(LOGIN_BUTTON).sendKeys(Keys.RETURN);

    try {
      WebDriverWait shortWait = new WebDriverWait(driver, PROCEED_CHECK_WAIT);
      t0 = System.currentTimeMillis();
      shortWait.until(ExpectedConditions.elementToBeClickable(PROCEED_BUTTON));
      logger.debug(
          "[TIMING] UOBLogin: existing-session Proceed dialog appeared: {}ms",
          System.currentTimeMillis() - t0);
      driver.findElement(PROCEED_BUTTON).click();
    } catch (TimeoutException ignored) {
      logger.debug("[TIMING] UOBLogin: no existing-session dialog (normal login)");
    }
  }
}
