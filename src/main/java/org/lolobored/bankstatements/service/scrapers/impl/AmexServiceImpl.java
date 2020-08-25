package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.service.conversion.AmexCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.AmexService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmexServiceImpl implements AmexService {

    private Logger logger = LoggerFactory.getLogger(AmexServiceImpl.class);

    @Autowired
    private AmexCSVConversionService amexCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {
        List<Statement> statements = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(webDriver, bank.getWaitTime());

        /**
         * Delete the download directory
         */
        File downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        /**
         * Login to the main page
         */
        logger.info("Connecting to " + bank.getConnectionUrl());

        webDriver.get(bank.getConnectionUrl());

        logger.info("Connected to " + bank.getConnectionUrl());

        /**
         * Look for the username
         * and password
         */
        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("eliloUserID"))));
        WebElement loginField = webDriver.findElement(By.id("eliloUserID"));
        loginField.sendKeys(bank.getUsername());

        logger.info("Username field [" + bank.getUsername() + "]");

        wait.until(ExpectedConditions.visibilityOf(webDriver.findElement(By.id("eliloPassword"))));
        WebElement passwordField = webDriver.findElement(By.id("eliloPassword"));
        passwordField.sendKeys(bank.getPassword());

        logger.info("Password field [" + bank.getPassword() + "]");

        /**
         * Find the login button
         * on the form and hit it
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.id("loginSubmit")));
        WebElement loginButton = webDriver.findElement(By.id("loginSubmit"));
        loginButton.submit();


        wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-block")));

        /**
         * Now let's go to the statements page
         */

        webDriver.navigate().to("https://global.americanexpress.com/activity/search");
        wait = new WebDriverWait(webDriver, bank.getWaitTime());

        /**
         * There might be a cookie confirmation remaining
         * panel preventing to click on the buttons
         */
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("sprite-AcceptButton_EN")));
            WebElement cookiesButton = webDriver.findElement(By.id("sprite-AcceptButton_EN"));
            cookiesButton.click();
        } catch (Exception err) {
            logger.warn("No cookies window found");
        }

        /**
         * Select the year
         */
        wait.until(ExpectedConditions.elementToBeClickable(By.className("nav-link")));
        List<WebElement> navLinks = webDriver.findElements(By.className("nav-link"));
        for (WebElement navLink : navLinks) {
            if ("View By Year".equalsIgnoreCase(navLink.getAttribute("title"))){
                System.out.println("ici");
                navLink.click();
                // wait for the download button to appear
                List<WebElement> yearNavLinks = navLink.findElement(By.xpath("./.."))
                        .findElements(By.className("nav-link"));
                // remove the original navlink
                yearNavLinks.remove(navLink);
                for (int i= 0; i< yearNavLinks.size(); i++) {
                    // workaround to refresh the links
                    yearNavLinks = navLink.findElement(By.xpath("./.."))
                            .findElements(By.className("nav-link"));
                    // remove the original navlink
                    yearNavLinks.remove(navLink);
                    WebElement yearNavLink = yearNavLinks.get(i);
                    yearNavLink.click();
                    wait.until(ExpectedConditions.elementToBeClickable(By.className("dls-icon-download")));
                    WebElement download = webDriver.findElement(By.className("dls-icon-download"));
                    download.sendKeys(Keys.RETURN);

                    WebElement popup = webDriver.findElement(By.className("axp-activity-download__DownloadModal__downloadModal___2WSh8"));

                    WebElement csvDownloadButton = popup.findElement(By.id("axp-activity-download-body-selection-options-type_csv"));
                    csvDownloadButton.click();
                    WebElement includeAll= popup.findElement(By.id("axp-activity-download-body-checkbox-options-includeAll"));
                    includeAll.click();

                    download = popup.findElement(By.className("btn-primary"));
                    download.click();

                    String accountName = webDriver.findElement(By.className("axp-account-switcher__accountSwitcher__lastFive___1s6L_")).getText();

                    String csvContent = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
                    statements.add(amexCSVConversionService.convertCSVToTransactions(accountName, Statement.CREDIT_CARD, csvContent));
                }
                break;
            }
        }
        return statements;
    }
}
