package org.lolobored.bankstatements.service.scrapers.pages.amex;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;

public class AmexActivityPage {

    private static final String ACTIVITY_URL = "https://global.americanexpress.com/activity/search";

    private static final By COOKIE_BUTTON  = By.id("sprite-AcceptButton_EN");
    private static final By NAV_LINKS      = By.className("nav-link");
    private static final By DOWNLOAD_ICON  = By.className("dls-icon-download");
    private static final By DOWNLOAD_MODAL = By.className("axp-activity-download__DownloadModal__downloadModal___2WSh8");
    private static final By CSV_RADIO      = By.id("axp-activity-download-body-selection-options-type_csv");
    private static final By INCLUDE_ALL    = By.id("axp-activity-download-body-checkbox-options-includeAll");
    private static final By CONFIRM_BUTTON = By.className("btn-primary");
    private static final By ACCOUNT_NAME   = By.className("axp-account-switcher__accountSwitcher__lastFive___1s6L_");

    private static final String VIEW_BY_YEAR_TITLE = "View By Year";

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Retained across openYearNavigation() and downloadCsvForYearIndex() calls
    private WebElement viewByYearLink;
    private WebElement yearNavParent;

    public AmexActivityPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void navigateToActivity() {
        driver.navigate().to(ACTIVITY_URL);
    }

    public void dismissCookieBanner() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(COOKIE_BUTTON));
            driver.findElement(COOKIE_BUTTON).click();
        } catch (TimeoutException ignored) {
        }
    }

    public String getAccountName() {
        return driver.findElement(ACCOUNT_NAME).getText();
    }

    /**
     * Clicks "View By Year", stores the parent element, and returns the number of year tabs.
     * Must be called before {@link #downloadCsvForYearIndex(int)}.
     */
    public int openYearNavigation() {
        wait.until(ExpectedConditions.elementToBeClickable(NAV_LINKS));
        for (WebElement navLink : driver.findElements(NAV_LINKS)) {
            if (VIEW_BY_YEAR_TITLE.equalsIgnoreCase(navLink.getAttribute("title"))) {
                navLink.click();
                viewByYearLink = navLink;
                yearNavParent = navLink.findElement(By.xpath("./.."));
                return getYearLinks().size();
            }
        }
        return 0;
    }

    /**
     * Refreshes the year link list (DOM updates after each click), selects by index,
     * then downloads via the modal dialog.
     */
    public void downloadCsvForYearIndex(int index) {
        List<WebElement> yearLinks = getYearLinks();
        yearLinks.get(index).click();

        wait.until(ExpectedConditions.elementToBeClickable(DOWNLOAD_ICON));
        driver.findElement(DOWNLOAD_ICON).sendKeys(Keys.RETURN);

        WebElement modal = driver.findElement(DOWNLOAD_MODAL);
        new Actions(driver).moveToElement(modal.findElement(CSV_RADIO)).click().perform();
        modal.findElement(INCLUDE_ALL).click();
        modal.findElement(CONFIRM_BUTTON).click();
    }

    private List<WebElement> getYearLinks() {
        List<WebElement> all = new ArrayList<>(yearNavParent.findElements(NAV_LINKS));
        all.remove(viewByYearLink);
        return all;
    }
}
