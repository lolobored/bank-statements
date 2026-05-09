package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.WestpacCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.WestpacService;
import org.lolobored.bankstatements.service.scrapers.pages.westpac.WestpacExportPage;
import org.lolobored.bankstatements.service.scrapers.pages.westpac.WestpacLoginPage;
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
public class WestpacServiceImpl implements WestpacService {

    @Autowired
    private WestpacCSVConversionService westpacCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {
        List<Statement> statements = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));

        File downloads = new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        WestpacLoginPage loginPage = new WestpacLoginPage(webDriver, wait);
        loginPage.login(bank.getConnectionUrl(), bank.getUsername(), bank.getPassword());

        WestpacExportPage exportPage = new WestpacExportPage(webDriver, wait);
        exportPage.navigateToExport();
        exportPage.downloadCsv();

        String csv = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
        statements.add(westpacCSVConversionService.convertTableToTransactions(
                bank.getAccounts().get(0).getAccountId(), Statement.CREDIT_CARD, csv));

        return statements;
    }
}
