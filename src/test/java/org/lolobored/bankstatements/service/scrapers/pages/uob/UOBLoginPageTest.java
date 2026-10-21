package org.lolobored.bankstatements.service.scrapers.pages.uob;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

class UOBLoginPageTest {

  private static final String URL = "https://pib.uob.com.sg/login";
  private static final By USERNAME_FIELD = By.cssSelector("input[placeholder='USERNAME']");
  private static final By LOG_IN_AGAIN_BUTTON =
      By.xpath("//button[normalize-space()='Log in again']");

  private WebDriver driver;
  private WebElement element;
  private AtomicInteger pageLoads;
  private UOBLoginPage loginPage;

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class);
    element = mock(WebElement.class);
    when(element.isDisplayed()).thenReturn(true);
    when(element.isEnabled()).thenReturn(true);
    when(driver.getCurrentUrl()).thenReturn("https://pib.uob.com.sg/accountsDashboard");
    when(driver.findElements(any(By.class))).thenReturn(List.of());

    pageLoads = new AtomicInteger();
    doAnswer(inv -> pageLoads.incrementAndGet()).when(driver).get(URL);

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1), Duration.ofMillis(10));
    loginPage = new UOBLoginPage(driver, wait, Duration.ofMillis(200));
  }

  /** The login form only renders once the page has been loaded {@code loadsNeeded} times. */
  private void formRendersAfterLoads(int loadsNeeded) {
    when(driver.findElement(any(By.class)))
        .thenAnswer(
            inv -> {
              By by = inv.getArgument(0);
              if (by.equals(LOG_IN_AGAIN_BUTTON)
                  || (by.equals(USERNAME_FIELD) && pageLoads.get() < loadsNeeded)) {
                throw new NoSuchElementException("blank page");
              }
              return element;
            });
  }

  @Test
  void logsInWhenFormRendersOnFirstLoad() {
    formRendersAfterLoads(1);

    loginPage.login(URL, "user", "pass");

    verify(driver, times(1)).get(URL);
    verify(element).sendKeys("user");
    verify(element).sendKeys("pass");
  }

  @Test
  void reloadsWhenLoginFormFailsToRender() {
    formRendersAfterLoads(2);

    loginPage.login(URL, "user", "pass");

    verify(driver, times(2)).get(URL);
    verify(element).sendKeys("user");
  }

  @Test
  void givesUpAfterMaxLoadAttempts() {
    formRendersAfterLoads(Integer.MAX_VALUE);

    assertThatThrownBy(() -> loginPage.login(URL, "user", "pass"))
        .isInstanceOf(TimeoutException.class);
    verify(driver, times(3)).get(URL);
  }
}
