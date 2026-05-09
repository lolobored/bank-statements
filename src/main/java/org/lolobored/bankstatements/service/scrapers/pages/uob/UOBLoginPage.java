package org.lolobored.bankstatements.service.scrapers.pages.uob;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class UOBLoginPage {

    private static final By USERNAME_FIELD = By.id("userName");
    private static final By PASSWORD_FIELD = By.id("PASSWORD1");
    private static final By LOGIN_BUTTON   = By.id("btnSubmit");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public UOBLoginPage(WebDriver driver, WebDriverWait wait) {
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
    }
}
