package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.AmexCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.AmexService;
import org.lolobored.bankstatements.service.scrapers.pages.amex.AmexActivityPage;
import org.lolobored.bankstatements.service.scrapers.pages.amex.AmexLoginPage;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmexServiceImpl implements AmexService {

    @Autowired
    private AmexCSVConversionService amexCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {
        List<Statement> statements = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));

        File downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        AmexLoginPage loginPage = new AmexLoginPage(webDriver, wait);
        loginPage.login(bank.getConnectionUrl(), bank.getUsername(), bank.getPassword(), bank.getSecurityCode());

        AmexActivityPage activityPage = new AmexActivityPage(webDriver, wait);
        activityPage.navigateToActivity();
        activityPage.dismissCookieBanner();

        int yearCount = activityPage.openYearNavigation();
        for (int i = 0; i < yearCount; i++) {
            activityPage.downloadCsvForYearIndex(i);
            String accountName = activityPage.getAccountName();
            String csv = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
            statements.add(amexCSVConversionService.convertTableToTransactions(
                    accountName, Statement.CREDIT_CARD, csv));
        }

        return statements;
    }
}
