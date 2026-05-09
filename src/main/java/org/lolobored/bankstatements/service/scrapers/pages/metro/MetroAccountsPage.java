package org.lolobored.bankstatements.service.scrapers.pages.metro;

import java.util.ArrayList;
import java.util.List;
import org.lolobored.bankstatements.model.Statement;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class MetroAccountsPage {

  private static final By ACCOUNT_HEADERS =
      By.xpath(
          "/html/body/form[1]/div[4]/div[3]/div[1]/div[2]/div[1]/div[1]/div/div[2]/div/div/div/div[1]/header");
  private static final By ACCOUNT_NUMBER =
      By.xpath("div[2]/div/div[2]/div[1]/div[2]/div/div/div[3]/div/span");
  private static final By ACCOUNT_ICON = By.xpath("div[1]/div/div/div/div/span");
  private static final By ACCOUNT_LINK =
      By.xpath("div[2]/div/div[1]/div[1]/div[1]/div[1]/div/div/div[1]/div/a");
  private static final By DOWNLOAD_BUTTON = By.id("BUT_81752B75369FA043129962");
  private static final By BACK_BUTTON = By.id("BUT_AA76E5E393F0103923812");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public MetroAccountsPage(WebDriver driver, WebDriverWait wait) {
    this.driver = driver;
    this.wait = wait;
  }

  public List<MetroAccountInfo> collectAccounts() {
    List<MetroAccountInfo> result = new ArrayList<>();

    for (WebElement header : driver.findElements(ACCOUNT_HEADERS)) {
      String accountNumber = header.findElement(ACCOUNT_NUMBER).getText();
      String accountType =
          "C".equals(header.findElement(ACCOUNT_ICON).getAttribute("data-icon"))
              ? Statement.CREDIT_CARD
              : Statement.DEBIT_ACCOUNT;
      String linkId = header.findElement(ACCOUNT_LINK).getAttribute("id");
      result.add(new MetroAccountInfo(accountNumber, accountType, linkId));
    }

    return result;
  }

  /** Re-finds the link by stored ID to avoid stale element references after navigation. */
  public void openAccountById(String linkId) {
    driver.findElement(By.id(linkId)).sendKeys(Keys.RETURN);
  }

  public void downloadStatements() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(DOWNLOAD_BUTTON));
    driver.findElement(DOWNLOAD_BUTTON).sendKeys(Keys.RETURN);
  }

  public void goBack() {
    wait.until(ExpectedConditions.elementToBeClickable(BACK_BUTTON));
    driver.findElement(BACK_BUTTON).sendKeys(Keys.RETURN);
  }
}
