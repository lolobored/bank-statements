package org.lolobored.bankstatements.service.scrapers.pages.metro;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class MetroLoginPage {

  private static final By USERNAME_FIELD = By.id("USER_NAME");
  private static final By COOKIE_BUTTON = By.id("js-mbCookieNotice-button");
  private static final By SUBMIT_BUTTON = By.className("mat-flat-button");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public MetroLoginPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void submitUsername(String url, String username) {
    driver.get(url);

    wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD));
    driver.findElement(USERNAME_FIELD).sendKeys(username);

    try {
      wait.until(ExpectedConditions.elementToBeClickable(COOKIE_BUTTON));
      driver.findElement(COOKIE_BUTTON).click();
    } catch (TimeoutException ignored) {
    }

    wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON));
    driver.findElement(SUBMIT_BUTTON).click();
  }
}
