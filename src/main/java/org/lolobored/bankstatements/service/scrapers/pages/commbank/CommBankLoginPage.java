package org.lolobored.bankstatements.service.scrapers.pages.commbank;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CommBankLoginPage {

    private static final By USERNAME_FIELD = By.id("txtMyClientNumber_field");
    private static final By PASSWORD_FIELD = By.id("txtMyPassword_field");
    private static final By LOGIN_BUTTON   = By.id("btnLogon_field");
    private static final By COOKIE_BUTTON  = By.id("tt_prominenceButtonNo");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public CommBankLoginPage(WebDriver driver, WebDriverWait wait) {
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

        try {
            wait.until(ExpectedConditions.elementToBeClickable(COOKIE_BUTTON));
            driver.findElement(COOKIE_BUTTON).sendKeys(Keys.RETURN);
        } catch (TimeoutException ignored) {
        }
    }
}
