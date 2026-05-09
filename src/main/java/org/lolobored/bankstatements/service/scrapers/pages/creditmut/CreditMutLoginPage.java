package org.lolobored.bankstatements.service.scrapers.pages.creditmut;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreditMutLoginPage {

    private static final By USERNAME_FIELD  = By.id("_userid");
    private static final By PASSWORD_FIELD  = By.id("_pwduser");
    private static final By LOGIN_BUTTON    = By.cssSelector("#login-submit > a");
    private static final By COOKIE_BUTTON   = By.xpath("//*[@id=\"cookieLBmainbuttons\"]/span/span[1]/span/a");
    // This element appears once the user has confirmed login on their mobile app
    private static final By MFA_READY_CHECK = By.id("I0:s2.A3:C2:triggerLink");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public CreditMutLoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void login(String url, String username, String password) {
        driver.get(url);

        wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD));
        driver.findElement(USERNAME_FIELD).sendKeys(username);

        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
        driver.findElement(PASSWORD_FIELD).sendKeys(password);

        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
        driver.findElement(LOGIN_BUTTON).sendKeys(Keys.RETURN);

        wait.until(ExpectedConditions.elementToBeClickable(COOKIE_BUTTON));
        driver.findElement(COOKIE_BUTTON).click();

        wait.until(ExpectedConditions.elementToBeClickable(MFA_READY_CHECK));
    }
}
