package org.lolobored.bankstatements.service.scrapers.pages.commbank;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class CommBankAccountsPage {

    private static final By ACCOUNT_CARDS  = By.className("account-card");
    private static final By ACCOUNT_NUMBER = By.className("account-number");
    private static final By OPTIONS_BUTTON = By.className("options-button");
    private static final By ACTION_LINKS   = By.className("btn-action--subtle");
    private static final By CANCEL_BUTTON  = By.className("cancel-button");

    private static final String VIEW_TRANSACTIONS = "View Transactions";

    private final WebDriver driver;

    public CommBankAccountsPage(WebDriver driver) {
        this.driver = driver;
    }

    public List<CommBankAccountInfo> collectAccounts() {
        List<CommBankAccountInfo> result = new ArrayList<>();

        for (WebElement card : driver.findElements(ACCOUNT_CARDS)) {
            String rawNumber = card.findElement(ACCOUNT_NUMBER).getText();
            String accountNumber = StringUtils.substringAfter(rawNumber, " ").replace(" ", "");

            card.findElement(OPTIONS_BUTTON).click();

            for (WebElement link : card.findElements(ACTION_LINKS)) {
                if (VIEW_TRANSACTIONS.equalsIgnoreCase(link.getText())) {
                    String url = link.getAttribute("href");
                    card.findElement(CANCEL_BUTTON).click();
                    result.add(new CommBankAccountInfo(accountNumber, url));
                    break;
                }
            }
        }

        return result;
    }
}
