package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OCBCLoginPage {

  private static final Logger logger = LoggerFactory.getLogger(OCBCLoginPage.class);

  private static final By USERNAME_FIELD = By.id("textAccessCode");
  private static final By PASSWORD_FIELD = By.id("textLoginPin");
  private static final By LOGIN_BUTTON = By.id("cmdLogin");

  // OCBC animates the username field into focus — needs a short pause before typing
  private static final Duration ANIMATION_PAUSE = Duration.ofSeconds(2);
  // Extra delay before submitting to let any pre-login checks complete
  private static final Duration PRE_SUBMIT_PAUSE = Duration.ofSeconds(2);

  private final WebDriver driver;
  private final WebDriverWait wait;

  public OCBCLoginPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void login(String url, String username, String password) throws InterruptedException {
    driver.get(url);

    long t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD));
    logger.debug(
        "[TIMING] OCBCLogin: wait for username field visible: {}ms",
        System.currentTimeMillis() - t0);

    WebElement usernameField = driver.findElement(USERNAME_FIELD);
    t0 = System.currentTimeMillis();
    new Actions(driver).moveToElement(usernameField).pause(ANIMATION_PAUSE).build().perform();
    logger.debug(
        "[TIMING] OCBCLogin: hover+pause action (budget {}ms): {}ms",
        ANIMATION_PAUSE.toMillis(),
        System.currentTimeMillis() - t0);
    usernameField.sendKeys(username);

    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
    logger.debug(
        "[TIMING] OCBCLogin: wait for password field visible: {}ms",
        System.currentTimeMillis() - t0);
    driver.findElement(PASSWORD_FIELD).sendKeys(password);

    t0 = System.currentTimeMillis();
    wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
    logger.debug(
        "[TIMING] OCBCLogin: wait for login button clickable: {}ms",
        System.currentTimeMillis() - t0);

    t0 = System.currentTimeMillis();
    Thread.sleep(PRE_SUBMIT_PAUSE.toMillis());
    logger.debug(
        "[TIMING] OCBCLogin: pre-submit sleep (budget {}ms): {}ms",
        PRE_SUBMIT_PAUSE.toMillis(),
        System.currentTimeMillis() - t0);
    driver.findElement(LOGIN_BUTTON).click();
  }
}
