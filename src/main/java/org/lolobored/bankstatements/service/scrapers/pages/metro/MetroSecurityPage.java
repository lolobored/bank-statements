package org.lolobored.bankstatements.service.scrapers.pages.metro;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class MetroSecurityPage {

  private static final By PASSWORD_FIELD = By.className("ib-password");
  private static final By SEED_COVERS = By.className("ib-seed-cover");
  private static final By SEED_POS = By.className("ib-seed-pos");
  private static final By CONTINUE_BUTTON = By.className("continue");
  // Optional popup that occasionally appears before the confirmation step
  private static final By OPTIONAL_POPUP = By.id("BUT_2AA03D92C0DCF9481393455");
  private static final By CONFIRM_BUTTON = By.id("BUT_02D66B31BAAF4F15580719");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public MetroSecurityPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public void completeSecurityChallenge(String password, String securityPin) {
    char[] pinChars = securityPin.toCharArray();
    int seedIndex = 0;

    wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
    driver.findElement(PASSWORD_FIELD).sendKeys(password);

    wait.until(ExpectedConditions.elementToBeClickable(SEED_COVERS));
    List<WebElement> seedboxes = driver.findElements(SEED_COVERS);
    for (WebElement seedbox : seedboxes) {
      int pos = Integer.parseInt(seedbox.findElement(SEED_POS).getText().replaceAll("[^0-9]*", ""));
      seedbox
          .findElement(By.name("security" + seedIndex++))
          .sendKeys(String.valueOf(pinChars[pos - 1]));
    }

    wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BUTTON));
    driver.findElement(CONTINUE_BUTTON).sendKeys(Keys.RETURN);

    try {
      wait.until(ExpectedConditions.elementToBeClickable(OPTIONAL_POPUP));
      driver.findElement(OPTIONAL_POPUP).sendKeys(Keys.RETURN);
    } catch (TimeoutException ignored) {
    }

    wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_BUTTON));
    driver.findElement(CONFIRM_BUTTON).sendKeys(Keys.RETURN);
  }
}
