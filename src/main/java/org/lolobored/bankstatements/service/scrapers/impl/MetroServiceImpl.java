package org.lolobored.bankstatements.service.scrapers.impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.MetroCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.MetroService;
import org.lolobored.bankstatements.service.scrapers.pages.metro.MetroAccountInfo;
import org.lolobored.bankstatements.service.scrapers.pages.metro.MetroAccountsPage;
import org.lolobored.bankstatements.service.scrapers.pages.metro.MetroLoginPage;
import org.lolobored.bankstatements.service.scrapers.pages.metro.MetroSecurityPage;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetroServiceImpl implements MetroService {

  @Autowired private MetroCSVConversionService metroCSVConversionService;

  @Override
  public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir)
      throws InterruptedException, IOException, ParseException {
    WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));

    File downloads = new File(downloadDir);
    FileUtils.deleteDirectory(downloads);
    downloads.mkdirs();

    MetroLoginPage loginPage = new MetroLoginPage(webDriver, wait);
    loginPage.submitUsername(bank.getConnectionUrl(), bank.getUsername());

    MetroSecurityPage securityPage = new MetroSecurityPage(webDriver, wait);
    securityPage.completeSecurityChallenge(bank.getPassword(), bank.getSecurityPin());

    MetroAccountsPage accountsPage = new MetroAccountsPage(webDriver, wait);
    List<MetroAccountInfo> accounts = accountsPage.collectAccounts();

    List<Statement> statements = new ArrayList<>();
    for (MetroAccountInfo account : accounts) {
      accountsPage.openAccountById(account.getLinkId());
      accountsPage.downloadStatements();
      String csv = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
      statements.add(
          metroCSVConversionService.convertTableToTransactions(
              account.getAccountNumber(), account.getAccountType(), csv));
      accountsPage.goBack();
    }

    return statements;
  }
}
