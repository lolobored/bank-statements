package org.lolobored.bankstatements.service.scrapers.pages.ocbc;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OCBCLoginPage {

    private static final By USERNAME_FIELD = By.id("textAccessCode");
    private static final By PASSWORD_FIELD = By.id("textLoginPin");
    private static final By LOGIN_BUTTON   = By.id("cmdLogin");

    // OCBC animates the username field into focus — needs a short pause before typing
    private static final Duration ANIMATION_PAUSE = Duration.ofSeconds(2);
    // Extra delay before submitting to let any pre-login checks complete
    private static final Duration PRE_SUBMIT_PAUSE = Duration.ofSeconds(5);

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OCBCLoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void login(String url, String username, String password) throws InterruptedException {
        driver.get(url);

        wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD));
        WebElement usernameField = driver.findElement(USERNAME_FIELD);
        new Actions(driver).moveToElement(usernameField).pause(ANIMATION_PAUSE).build().perform();
        Thread.sleep(ANIMATION_PAUSE.toMillis());
        usernameField.sendKeys(username);

        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
        driver.findElement(PASSWORD_FIELD).sendKeys(password);

        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
        Thread.sleep(PRE_SUBMIT_PAUSE.toMillis());
        driver.findElement(LOGIN_BUTTON).click();
    }
}
